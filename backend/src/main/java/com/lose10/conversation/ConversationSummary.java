package com.lose10.conversation;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversation_summaries")
public class ConversationSummary extends BaseEntity {

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "summary_text", nullable = false, columnDefinition = "text")
    private String summaryText;

    protected ConversationSummary() {
    }

    public ConversationSummary(UUID threadId, Instant periodStart, Instant periodEnd, String summaryText) {
        this.threadId = threadId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.summaryText = summaryText;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public Instant getPeriodEnd() {
        return periodEnd;
    }
}
