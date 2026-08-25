package com.lose10.memory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * The one place in the codebase that writes a raw pgvector similarity
 * query (docs/DATABASE.md §4, docs/MEMORY.md §7). The query vector is
 * passed as a bracketed literal string (e.g. "[0.1,0.2,...]") cast to
 * {@code vector} in SQL, rather than trying to get Hibernate's native-query
 * parameter binder to understand {@code float[]}/PGvector — simpler and
 * more portable than fighting JPA's native-query type inference for a
 * custom JDBC type.
 */
@Repository
public class MemorySemanticSearchRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @SuppressWarnings("unchecked")
    public List<UUID> findNearestMemoryIds(UUID userId, float[] queryVector, MemoryCategory category, int topK) {
        String vectorLiteral = toVectorLiteral(queryVector);
        String sql = """
                SELECT m.id FROM memories m
                JOIN memory_embeddings e ON e.memory_id = m.id
                WHERE m.user_id = ?1
                  AND (m.expires_at IS NULL OR m.expires_at > now())
                  AND (?2 IS NULL OR m.category = ?2)
                ORDER BY e.embedding <=> CAST(?3 AS vector)
                LIMIT ?4
                """;
        List<Object> rows = entityManager.createNativeQuery(sql)
                .setParameter(1, userId)
                .setParameter(2, category == null ? null : category.name())
                .setParameter(3, vectorLiteral)
                .setParameter(4, topK)
                .getResultList();
        return rows.stream().map(row -> UUID.fromString(row.toString())).toList();
    }

    private String toVectorLiteral(float[] vector) {
        StringBuilder sb = new StringBuilder(vector.length * 8);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        return sb.append(']').toString();
    }
}
