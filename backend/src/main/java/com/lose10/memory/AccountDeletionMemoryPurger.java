package com.lose10.memory;

import com.lose10.auth.event.UserDeletionRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Purges memory records on account deletion (docs/backlog L10-1004).
 * Embeddings and events cascade at the DB level from memories, so
 * deleting the parent MemoryRecord rows is sufficient.
 */
@Component
public class AccountDeletionMemoryPurger {

    private final MemoryRecordRepository memoryRepository;

    public AccountDeletionMemoryPurger(MemoryRecordRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @EventListener
    @Transactional
    public void onAccountDeletion(UserDeletionRequestedEvent event) {
        memoryRepository.deleteByUserId(event.userId());
    }
}
