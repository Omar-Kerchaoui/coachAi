package com.lose10.memory;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Entity
@Table(name = "memory_embeddings")
public class MemoryEmbedding extends BaseEntity {

    @Column(name = "memory_id", nullable = false)
    private UUID memoryId;

    @Type(VectorType.class)
    @Column(nullable = false, columnDefinition = "vector(1024)")
    private float[] embedding;

    @Column(nullable = false, length = 50)
    private String model;

    @Column(nullable = false)
    private int dimension;

    @Column(name = "embedding_version", nullable = false)
    private int embeddingVersion = 1;

    protected MemoryEmbedding() {
    }

    public MemoryEmbedding(UUID memoryId, float[] embedding, String model, int dimension) {
        this.memoryId = memoryId;
        this.embedding = embedding;
        this.model = model;
        this.dimension = dimension;
    }

    public void updateVector(float[] embedding, String model, int dimension) {
        this.embedding = embedding;
        this.model = model;
        this.dimension = dimension;
    }

    public UUID getMemoryId() {
        return memoryId;
    }

    public float[] getEmbedding() {
        return embedding;
    }
}
