package com.lose10.memory;

import com.lose10.ai.AIProviderException;
import com.lose10.ai.EmbeddingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Structured pre-filter + pgvector semantic search + weighted re-ranking,
 * capped to a small top-N (docs/MEMORY.md §6). Never returns every match
 * above a raw similarity threshold — always ranked and capped.
 */
@Service
public class MemoryRetriever {

    private static final Logger log = LoggerFactory.getLogger(MemoryRetriever.class);
    private static final int SEMANTIC_CANDIDATE_LIMIT = 20;

    private static final double SIMILARITY_WEIGHT = 0.5;
    private static final double IMPORTANCE_WEIGHT = 0.3;
    private static final double CONFIDENCE_WEIGHT = 0.2;

    private final MemoryRecordRepository memoryRepository;
    private final MemorySemanticSearchRepository semanticSearchRepository;
    private final EmbeddingProvider embeddingProvider;
    private final int maxRelevantMemories;

    public MemoryRetriever(
            MemoryRecordRepository memoryRepository,
            MemorySemanticSearchRepository semanticSearchRepository,
            EmbeddingProvider embeddingProvider,
            @Value("${lose10.ai.context.max-relevant-memories}") int maxRelevantMemories
    ) {
        this.memoryRepository = memoryRepository;
        this.semanticSearchRepository = semanticSearchRepository;
        this.embeddingProvider = embeddingProvider;
        this.maxRelevantMemories = maxRelevantMemories;
    }

    @Transactional
    public List<MemoryRecord> retrieve(UUID userId, String queryText, MemoryCategory categoryFilter) {
        List<UUID> rankedIds;
        try {
            EmbeddingProvider.Embedding queryEmbedding = embeddingProvider.generateEmbedding(queryText);
            rankedIds = semanticSearchRepository.findNearestMemoryIds(userId, queryEmbedding.vector(), categoryFilter, SEMANTIC_CANDIDATE_LIMIT);
        } catch (AIProviderException e) {
            log.warn("Embedding generation failed — falling back to importance/recency ranking without semantic search", e);
            return fallbackRetrieve(userId);
        }

        if (rankedIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, MemoryRecord> byId = new LinkedHashMap<>();
        memoryRepository.findAllById(rankedIds).forEach(m -> byId.put(m.getId(), m));

        List<MemoryRecord> top = rankedIds.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingDouble(m -> -score(m, rankedIds.indexOf(m.getId()), rankedIds.size())))
                .limit(maxRelevantMemories)
                .toList();

        top.forEach(MemoryRecord::markUsed);
        return top;
    }

    private List<MemoryRecord> fallbackRetrieve(UUID userId) {
        return memoryRepository.findActiveByUserId(userId, Instant.now()).stream()
                .sorted(Comparator.comparingDouble((MemoryRecord m) -> -(importanceScore(m.getImportance()) * IMPORTANCE_WEIGHT + m.getConfidence().doubleValue() * CONFIDENCE_WEIGHT)))
                .limit(maxRelevantMemories)
                .toList();
    }

    private double score(MemoryRecord memory, int rankPosition, int totalCandidates) {
        double similarityScore = 1.0 - ((double) rankPosition / Math.max(totalCandidates, 1));
        double importance = importanceScore(memory.getImportance());
        double confidence = memory.getConfidence().doubleValue();
        double recencyBonus = recencyBonus(memory.getLastUsedAt());
        return SIMILARITY_WEIGHT * similarityScore + IMPORTANCE_WEIGHT * importance + CONFIDENCE_WEIGHT * confidence + recencyBonus;
    }

    private double importanceScore(MemoryImportance importance) {
        return switch (importance) {
            case CRITICAL -> 1.0;
            case HIGH -> 0.75;
            case MEDIUM -> 0.5;
            case LOW -> 0.25;
        };
    }

    private double recencyBonus(Instant lastUsedAt) {
        if (lastUsedAt == null) {
            return 0.0;
        }
        boolean usedRecently = Duration.between(lastUsedAt, Instant.now()).toDays() < 7;
        return usedRecently ? 0.05 : 0.0;
    }
}
