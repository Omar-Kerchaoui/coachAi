package com.lose10.platform.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Base for entities that are updated after creation: adds an
 * auto-maintained updated_at timestamp on top of {@link BaseEntity}.
 */
@MappedSuperclass
public abstract class MutableEntity extends BaseEntity {

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
