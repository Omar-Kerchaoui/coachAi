package com.lose10.checkin;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges check-ins, daily check-ins, and weekly reviews on account
 * deletion (docs/backlog L10-1004).
 */
@Component
public class AccountDeletionCheckInPurger {

    private final CheckInRepository checkInRepository;
    private final DailyCheckInRepository dailyCheckInRepository;
    private final WeeklyReviewRepository weeklyReviewRepository;

    public AccountDeletionCheckInPurger(
            CheckInRepository checkInRepository,
            DailyCheckInRepository dailyCheckInRepository,
            WeeklyReviewRepository weeklyReviewRepository
    ) {
        this.checkInRepository = checkInRepository;
        this.dailyCheckInRepository = dailyCheckInRepository;
        this.weeklyReviewRepository = weeklyReviewRepository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        checkInRepository.deleteByUserId(event.userId());
        dailyCheckInRepository.deleteByUserId(event.userId());
        weeklyReviewRepository.deleteByUserId(event.userId());
    }
}
