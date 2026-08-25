package com.lose10.checkin;

import com.lose10.platform.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure check-in CRUD — deliberately has no dependency on the {@code ai}
 * module. Weekly review generation needs {@code AIOrchestrator}, but
 * {@code ContextBuilder} (in {@code ai}) needs this service for the
 * "recent check-in" context section; putting the AI dependency here too
 * would create ai → checkin → ai. See {@link WeeklyReviewGenerator} for
 * that piece instead.
 */
@Service
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final DailyCheckInRepository dailyCheckInRepository;
    private final WeeklyReviewRepository weeklyReviewRepository;

    public CheckInService(
            CheckInRepository checkInRepository,
            DailyCheckInRepository dailyCheckInRepository,
            WeeklyReviewRepository weeklyReviewRepository
    ) {
        this.checkInRepository = checkInRepository;
        this.dailyCheckInRepository = dailyCheckInRepository;
        this.weeklyReviewRepository = weeklyReviewRepository;
    }

    @Transactional
    public CheckIn create(UUID userId, CheckInType type, Instant scheduledFor) {
        return checkInRepository.save(new CheckIn(userId, type, scheduledFor));
    }

    @Transactional
    public CheckIn complete(UUID userId, UUID checkInId, Map<String, Object> responses) {
        CheckIn checkIn = checkInRepository.findByIdAndUserId(checkInId, userId)
                .orElseThrow(() -> ApiException.notFound("Check-in not found"));
        checkIn.complete(responses);

        if (checkIn.getCheckInType() != CheckInType.WEEKLY) {
            upsertDailyCheckIn(userId, responses);
        }
        return checkIn;
    }

    private void upsertDailyCheckIn(UUID userId, Map<String, Object> responses) {
        LocalDate today = Instant.now().atZone(ZoneOffset.UTC).toLocalDate();
        String mood = stringOrNull(responses.get("mood"));
        String energy = stringOrNull(responses.get("energy"));
        String adherence = stringOrNull(responses.get("adherence"));

        dailyCheckInRepository.findByUserIdAndCheckInDate(userId, today)
                .ifPresentOrElse(
                        existing -> { /* daily_check_ins is intentionally append-once per day; later completions this day are ignored */ },
                        () -> dailyCheckInRepository.save(new DailyCheckIn(userId, today, mood, energy, adherence))
                );
    }

    private String stringOrNull(Object value) {
        return value == null ? null : value.toString();
    }

    @Transactional(readOnly = true)
    public List<CheckIn> recent(UUID userId) {
        return checkInRepository.findTop20ByUserIdOrderByScheduledForDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<WeeklyReview> weeklyReviews(UUID userId) {
        return weeklyReviewRepository.findByUserIdOrderByWeekStartDesc(userId);
    }

    @Transactional
    WeeklyReview saveWeeklyReview(UUID userId, LocalDate weekStart, String summaryText) {
        return weeklyReviewRepository.save(new WeeklyReview(userId, weekStart, summaryText, "AI"));
    }
}
