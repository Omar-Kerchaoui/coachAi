package com.lose10.checkin;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "weekly_reviews")
public class WeeklyReview extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "summary_text", nullable = false, columnDefinition = "text")
    private String summaryText;

    @Column(name = "generated_by", nullable = false, length = 10)
    private String generatedBy = "AI";

    protected WeeklyReview() {
    }

    public WeeklyReview(UUID userId, LocalDate weekStart, String summaryText, String generatedBy) {
        this.userId = userId;
        this.weekStart = weekStart;
        this.summaryText = summaryText;
        this.generatedBy = generatedBy;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getWeekStart() {
        return weekStart;
    }

    public String getSummaryText() {
        return summaryText;
    }
}
