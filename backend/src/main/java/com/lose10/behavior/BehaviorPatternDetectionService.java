package com.lose10.behavior;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Day-of-week clustering over a user's recent {@link BehaviorEvent} stream
 * (docs/MEMORY.md §10 — "recurring missed workouts", "Friday_training_
 * adherence = low", etc.). Split out from {@link BehaviorPatternDetectionJob}
 * so it's invoked through a real Spring proxy rather than a same-class
 * self-invocation (see docs/backlog E10 hardening notes on this exact class
 * of bug, caught earlier in ProgressSnapshotJob/RefreshTokenService).
 */
@Service
public class BehaviorPatternDetectionService {

    private static final int LOOKBACK_DAYS = 42;
    private static final int MIN_OCCURRENCES_FOR_PATTERN = 3;

    private final BehaviorEventRepository eventRepository;
    private final BehaviorPatternRepository patternRepository;

    public BehaviorPatternDetectionService(BehaviorEventRepository eventRepository, BehaviorPatternRepository patternRepository) {
        this.eventRepository = eventRepository;
        this.patternRepository = patternRepository;
    }

    @Transactional
    public void detectForUser(UUID userId) {
        Instant since = Instant.now().minusSeconds(LOOKBACK_DAYS * 86_400L);

        Map<String, List<BehaviorEvent>> byType = allEventTypesSince(userId, since);
        byType.forEach((eventType, events) -> detectDayOfWeekClustering(userId, eventType, events));
    }

    private Map<String, List<BehaviorEvent>> allEventTypesSince(UUID userId, Instant since) {
        // Grouped in-memory rather than a per-type query — event type
        // cardinality per user is small, and this runs once nightly per
        // user, not per request.
        return eventRepository.findByUserIdAndCreatedAtAfter(userId, since).stream()
                .collect(Collectors.groupingBy(BehaviorEvent::getEventType));
    }

    private void detectDayOfWeekClustering(UUID userId, String eventType, List<BehaviorEvent> events) {
        Map<DayOfWeek, Long> countsByDay = events.stream()
                .collect(Collectors.groupingBy(e -> e.occurredAt().atZone(ZoneOffset.UTC).getDayOfWeek(), Collectors.counting()));

        countsByDay.forEach((day, count) -> {
            if (count < MIN_OCCURRENCES_FOR_PATTERN) {
                return;
            }
            String patternKey = (eventType + "_" + day.name()).toLowerCase(Locale.ROOT);
            String dayName = day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            String description = "Recurring " + eventType.toLowerCase(Locale.ROOT).replace('_', ' ') + " pattern on " + dayName + "s.";
            BigDecimal confidence = confidenceFor(count);

            patternRepository.findByUserIdAndPatternKey(userId, patternKey)
                    .ifPresentOrElse(
                            existing -> existing.reinforce(confidence),
                            () -> patternRepository.save(new BehaviorPattern(userId, patternKey, description, confidence))
                    );
        });
    }

    private BigDecimal confidenceFor(long occurrences) {
        double confidence = Math.min(0.5 + (occurrences - MIN_OCCURRENCES_FOR_PATTERN) * 0.1, 0.95);
        return BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP);
    }
}
