package com.lose10.analytics;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "business_metrics_daily")
public class BusinessMetricsDaily extends BaseEntity {

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Column(name = "mrr_cents", nullable = false)
    private long mrrCents;

    @Column(name = "arr_cents", nullable = false)
    private long arrCents;

    @Column(name = "active_subscribers", nullable = false)
    private int activeSubscribers;

    @Column(name = "trialing_subscribers", nullable = false)
    private int trialingSubscribers;

    @Column(name = "new_subscribers", nullable = false)
    private int newSubscribers;

    @Column(name = "churned_subscribers", nullable = false)
    private int churnedSubscribers;

    @Column(name = "trial_conversions", nullable = false)
    private int trialConversions;

    @Column(nullable = false)
    private int dau;

    @Column(nullable = false)
    private int wau;

    @Column(name = "stalled_users", nullable = false)
    private int stalledUsers;

    @Column(name = "at_risk_users", nullable = false)
    private int atRiskUsers;

    @Column(name = "milestones_reached", nullable = false)
    private int milestonesReached;

    protected BusinessMetricsDaily() {
    }

    public BusinessMetricsDaily(
            LocalDate metricDate, long mrrCents, long arrCents, int activeSubscribers, int trialingSubscribers,
            int newSubscribers, int churnedSubscribers, int trialConversions, int dau, int wau,
            int stalledUsers, int atRiskUsers, int milestonesReached
    ) {
        this.metricDate = metricDate;
        this.mrrCents = mrrCents;
        this.arrCents = arrCents;
        this.activeSubscribers = activeSubscribers;
        this.trialingSubscribers = trialingSubscribers;
        this.newSubscribers = newSubscribers;
        this.churnedSubscribers = churnedSubscribers;
        this.trialConversions = trialConversions;
        this.dau = dau;
        this.wau = wau;
        this.stalledUsers = stalledUsers;
        this.atRiskUsers = atRiskUsers;
        this.milestonesReached = milestonesReached;
    }

    /**
     * Re-running the rollup for a day that already has a row (retry,
     * overlapping run) updates in place rather than creating a duplicate —
     * same idempotency shape as {@code ProgressSnapshot.update}.
     */
    public void update(
            long mrrCents, long arrCents, int activeSubscribers, int trialingSubscribers,
            int newSubscribers, int churnedSubscribers, int trialConversions, int dau, int wau,
            int stalledUsers, int atRiskUsers, int milestonesReached
    ) {
        this.mrrCents = mrrCents;
        this.arrCents = arrCents;
        this.activeSubscribers = activeSubscribers;
        this.trialingSubscribers = trialingSubscribers;
        this.newSubscribers = newSubscribers;
        this.churnedSubscribers = churnedSubscribers;
        this.trialConversions = trialConversions;
        this.dau = dau;
        this.wau = wau;
        this.stalledUsers = stalledUsers;
        this.atRiskUsers = atRiskUsers;
        this.milestonesReached = milestonesReached;
    }

    public LocalDate getMetricDate() {
        return metricDate;
    }

    public long getMrrCents() {
        return mrrCents;
    }

    public long getArrCents() {
        return arrCents;
    }

    public int getActiveSubscribers() {
        return activeSubscribers;
    }

    public int getTrialingSubscribers() {
        return trialingSubscribers;
    }

    public int getNewSubscribers() {
        return newSubscribers;
    }

    public int getChurnedSubscribers() {
        return churnedSubscribers;
    }

    public int getTrialConversions() {
        return trialConversions;
    }

    public int getDau() {
        return dau;
    }

    public int getWau() {
        return wau;
    }

    public int getStalledUsers() {
        return stalledUsers;
    }

    public int getAtRiskUsers() {
        return atRiskUsers;
    }

    public int getMilestonesReached() {
        return milestonesReached;
    }
}
