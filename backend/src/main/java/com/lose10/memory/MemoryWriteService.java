package com.lose10.memory;

import com.lose10.ai.AIProviderException;
import com.lose10.ai.EmbeddingProvider;
import com.lose10.analytics.AnalyticsEventType;
import com.lose10.analytics.AnalyticsRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The single create-or-update-with-contradiction-detection path for
 * memories (docs/MEMORY.md §4). Both {@link MemoryExtractor} (after its own
 * AI-based candidate detection) and the {@code saveMemory}/
 * {@code updateMemory} AI tools (docs/AI_ARCHITECTURE.md §4 — "goes through
 * the same extraction/dedup rules as async extraction") funnel through
 * here, so the dedup/contradiction logic exists in exactly one place.
 */
@Service
public class MemoryWriteService {

    private static final Logger log = LoggerFactory.getLogger(MemoryWriteService.class);

    private final MemoryRecordRepository memoryRepository;
    private final MemoryEventRepository eventRepository;
    private final MemoryEmbeddingRepository embeddingRepository;
    private final EmbeddingProvider embeddingProvider;
    private final AnalyticsRecorder analyticsRecorder;

    public MemoryWriteService(
            MemoryRecordRepository memoryRepository,
            MemoryEventRepository eventRepository,
            MemoryEmbeddingRepository embeddingRepository,
            EmbeddingProvider embeddingProvider,
            AnalyticsRecorder analyticsRecorder
    ) {
        this.memoryRepository = memoryRepository;
        this.eventRepository = eventRepository;
        this.embeddingRepository = embeddingRepository;
        this.embeddingProvider = embeddingProvider;
        this.analyticsRecorder = analyticsRecorder;
    }

    @Transactional
    public MemoryRecord upsert(UUID userId, MemoryCategory category, String key, String content,
                                MemoryImportance importance, BigDecimal confidence, UUID sourceMessageId) {
        MemoryRecord record = memoryRepository.findByUserIdAndCategoryAndKey(userId, category, key)
                .map(existing -> updateExisting(userId, existing, content, confidence))
                .orElseGet(() -> createNew(userId, category, key, content, importance, confidence, sourceMessageId));

        generateAndStoreEmbedding(record);
        return record;
    }

    private MemoryRecord createNew(UUID userId, MemoryCategory category, String key, String content,
                                    MemoryImportance importance, BigDecimal confidence, UUID sourceMessageId) {
        MemoryRecord record = memoryRepository.save(new MemoryRecord(userId, category, key, content, importance, confidence, sourceMessageId));
        eventRepository.save(new MemoryEvent(record.getId(), MemoryEvent.EventType.CREATED, null));
        analyticsRecorder.record(AnalyticsEventType.MEMORY_CREATED, userId);
        return record;
    }

    private MemoryRecord updateExisting(UUID userId, MemoryRecord existing, String newContent, BigDecimal newConfidence) {
        String previousContent = existing.getContent();
        boolean contradicts = !previousContent.equalsIgnoreCase(newContent);
        existing.updateContent(newContent, newConfidence);
        eventRepository.save(new MemoryEvent(
                existing.getId(),
                contradicts ? MemoryEvent.EventType.CONTRADICTED : MemoryEvent.EventType.UPDATED,
                previousContent
        ));
        analyticsRecorder.record(AnalyticsEventType.MEMORY_UPDATED, userId);
        return existing;
    }

    private void generateAndStoreEmbedding(MemoryRecord record) {
        try {
            EmbeddingProvider.Embedding embedding = embeddingProvider.generateEmbedding(record.getContent());
            embeddingRepository.findByMemoryId(record.getId())
                    .ifPresentOrElse(
                            existing -> existing.updateVector(embedding.vector(), embedding.model(), embedding.dimension()),
                            () -> embeddingRepository.save(new MemoryEmbedding(record.getId(), embedding.vector(), embedding.model(), embedding.dimension()))
                    );
        } catch (AIProviderException e) {
            log.error("Failed to generate embedding for memory {}", record.getId(), e);
        }
    }
}
