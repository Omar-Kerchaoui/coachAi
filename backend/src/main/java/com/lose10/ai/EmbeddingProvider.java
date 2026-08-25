package com.lose10.ai;

import java.util.List;

/**
 * Abstraction over embedding generation (docs/MEMORY.md §7, §9). Only the
 * {@code memory} module's extraction/retrieval pipeline should depend on
 * this — no other module generates embeddings directly.
 */
public interface EmbeddingProvider {

    Embedding generateEmbedding(String text);

    List<Embedding> batchGenerateEmbeddings(List<String> texts);

    record Embedding(float[] vector, String model, int dimension) {
    }
}
