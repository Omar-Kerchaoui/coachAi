package com.lose10.admin;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges admin notes and human-intervention records written about a
 * deleted user (docs/backlog L10-1004) — the {@code adminId} on these
 * rows belongs to a different (admin) user and is left untouched.
 */
@Component
public class AccountDeletionAdminRecordsPurger {

    private final AdminNoteRepository noteRepository;
    private final HumanInterventionRepository interventionRepository;

    public AccountDeletionAdminRecordsPurger(AdminNoteRepository noteRepository, HumanInterventionRepository interventionRepository) {
        this.noteRepository = noteRepository;
        this.interventionRepository = interventionRepository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        noteRepository.deleteByUserId(event.userId());
        interventionRepository.deleteByUserId(event.userId());
    }
}
