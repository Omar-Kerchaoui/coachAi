package com.lose10.checkin;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeeklyReviewRepository extends JpaRepository<WeeklyReview, UUID> {
    List<WeeklyReview> findByUserIdOrderByWeekStartDesc(UUID userId);
}
