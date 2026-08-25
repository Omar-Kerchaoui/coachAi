package com.lose10.nutrition;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "nutrition_logs")
public class NutritionLog extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    protected NutritionLog() {
    }

    public NutritionLog(UUID userId, LocalDate logDate) {
        this.userId = userId;
        this.logDate = logDate;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getLogDate() {
        return logDate;
    }
}
