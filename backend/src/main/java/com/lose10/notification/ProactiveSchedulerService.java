package com.lose10.notification;

import com.lose10.coaching.CoachingStateService;
import com.lose10.profile.ProfileService;
import com.lose10.whatsapp.WhatsAppAccount;
import com.lose10.whatsapp.WhatsAppAccountRepository;
import com.lose10.whatsapp.WhatsAppProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Per-user due-check and send, split out from {@link ProactiveSchedulerJob}
 * so it runs through a real Spring proxy (transactional + testable in
 * isolation) — the job itself only handles iteration and locking
 * (docs/backlog L10-710, L10-711). Currently implements MORNING_CHECKIN
 * concretely; the remaining message types (docs/WHATSAPP.md §4) plug into
 * the same due-check + idempotent-claim + gate + send mechanism.
 */
@Service
public class ProactiveSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(ProactiveSchedulerService.class);
    private static final String MORNING_CHECKIN_TEXT = "Good morning! How are you feeling today?";

    private final NotificationPreferencesRepository preferencesRepository;
    private final ScheduledMessageRepository scheduledMessageRepository;
    private final WhatsAppAccountRepository whatsAppAccountRepository;
    private final CoachingStateService coachingStateService;
    private final ProfileService profileService;
    private final WhatsAppProvider whatsAppProvider;
    private final int maxProactivePerDay;

    public ProactiveSchedulerService(
            NotificationPreferencesRepository preferencesRepository,
            ScheduledMessageRepository scheduledMessageRepository,
            WhatsAppAccountRepository whatsAppAccountRepository,
            CoachingStateService coachingStateService,
            ProfileService profileService,
            WhatsAppProvider whatsAppProvider,
            @Value("${lose10.scheduler.max-proactive-messages-per-day}") int maxProactivePerDay
    ) {
        this.preferencesRepository = preferencesRepository;
        this.scheduledMessageRepository = scheduledMessageRepository;
        this.whatsAppAccountRepository = whatsAppAccountRepository;
        this.coachingStateService = coachingStateService;
        this.profileService = profileService;
        this.whatsAppProvider = whatsAppProvider;
        this.maxProactivePerDay = maxProactivePerDay;
    }

    /**
     * @param pollWindowMinutes how far back from "now" (in the user's local
     *                          time) counts as due — matched to the job's poll
     *                          interval so no due message is missed between runs.
     */
    @Transactional
    public void checkMorningCheckinForUser(UUID userId, int pollWindowMinutes) {
        NotificationPreferences prefs = preferencesRepository.findByUserId(userId).orElse(null);
        if (prefs == null || prefs.getMorningCheckinTime() == null) {
            return;
        }

        ZoneId zone = resolveTimezone(userId);
        if (zone == null) {
            return;
        }
        ZonedDateTime nowLocal = Instant.now().atZone(zone);
        LocalTime localTime = nowLocal.toLocalTime();

        boolean due = !localTime.isBefore(prefs.getMorningCheckinTime())
                && localTime.isBefore(prefs.getMorningCheckinTime().plusMinutes(pollWindowMinutes));
        if (!due) {
            return;
        }

        if (!passesGates(userId, prefs, localTime)) {
            return;
        }

        WhatsAppAccount account = whatsAppAccountRepository.findByUserId(userId).orElse(null);
        if (account == null || !account.canReceiveMessages()) {
            return;
        }

        claimAndSend(userId, account, nowLocal.toInstant(), zone);
    }

    private boolean passesGates(UUID userId, NotificationPreferences prefs, LocalTime localTime) {
        if (prefs.isWithinQuietHours(localTime)) {
            return false;
        }
        if (!coachingStateService.allowsAutomaticAiResponse(userId)) {
            return false;
        }
        Instant startOfDay = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        long sentToday = scheduledMessageRepository.countByUserIdAndScheduledForBetween(userId, startOfDay, Instant.now());
        return sentToday < maxProactivePerDay;
    }

    private void claimAndSend(UUID userId, WhatsAppAccount account, Instant scheduledFor, ZoneId userZone) {
        try {
            ScheduledMessage claimed = scheduledMessageRepository.save(
                    new ScheduledMessage(userId, MessageType.MORNING_CHECKIN, scheduledFor, userZone, Map.of()));

            WhatsAppProvider.SendResult result = whatsAppProvider.sendText(account.getPhoneE164(), MORNING_CHECKIN_TEXT);
            if (result.success()) {
                claimed.markSent();
            } else {
                claimed.markFailed();
                log.warn("Failed to send morning check-in to user {}: {}", userId, result.errorMessage());
            }
        } catch (DataIntegrityViolationException e) {
            // Unique constraint on (user_id, message_type, scheduled_for::date) —
            // another concurrent run (or instance) already claimed today's slot.
            log.debug("Morning check-in for user {} already claimed today — skipping", userId);
        }
    }

    private ZoneId resolveTimezone(UUID userId) {
        try {
            String timezone = profileService.getProfile(userId).timezone();
            return timezone != null ? ZoneId.of(timezone) : ZoneId.of("UTC");
        } catch (Exception e) {
            return null;
        }
    }
}
