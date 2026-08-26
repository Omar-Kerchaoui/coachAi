package com.lose10.memory;

import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import com.lose10.platform.error.ApiException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Locks in the user-initiated memory deletion contract (docs/backlog
 * L10-1005, docs/MEMORY.md §8): a real hard delete of both the structured
 * row and its embedding, audited — never a soft-delete, and never
 * deletable by a different user.
 */
class MemoryServiceTest {

    private final MemoryRecordRepository memoryRepository = mock(MemoryRecordRepository.class);
    private final MemoryEmbeddingRepository embeddingRepository = mock(MemoryEmbeddingRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final MemoryService memoryService = new MemoryService(memoryRepository, embeddingRepository, auditLogService);

    @Test
    void deleteHardDeletesTheRecordAndItsEmbeddingAndAudits() {
        UUID userId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        MemoryRecord record = new MemoryRecord(userId, MemoryCategory.PREFERENCE, "key", "content", MemoryImportance.MEDIUM, BigDecimal.valueOf(0.9), null);
        MemoryEmbedding embedding = mock(MemoryEmbedding.class);

        when(memoryRepository.findByIdAndUserId(memoryId, userId)).thenReturn(Optional.of(record));
        when(embeddingRepository.findByMemoryId(record.getId())).thenReturn(Optional.of(embedding));

        memoryService.delete(userId, memoryId);

        verify(memoryRepository).delete(record);
        verify(embeddingRepository).delete(embedding);
        verify(auditLogService).record(userId, AuditLog.ActorType.USER, "MEMORY_DELETED", "MemoryRecord", memoryId);
    }

    @Test
    void deleteOfAnotherUsersMemoryIsRejectedNotSilentlyNoOp() {
        UUID owner = UUID.randomUUID();
        UUID attacker = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();

        when(memoryRepository.findByIdAndUserId(memoryId, attacker)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memoryService.delete(attacker, memoryId)).isInstanceOf(ApiException.class);

        verify(memoryRepository, never()).delete(any());
    }

    @Test
    void deleteStillSucceedsWhenNoEmbeddingWasEverGenerated() {
        UUID userId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        MemoryRecord record = new MemoryRecord(userId, MemoryCategory.PREFERENCE, "key", "content", MemoryImportance.MEDIUM, BigDecimal.valueOf(0.9), null);

        when(memoryRepository.findByIdAndUserId(memoryId, userId)).thenReturn(Optional.of(record));
        when(embeddingRepository.findByMemoryId(record.getId())).thenReturn(Optional.empty());

        memoryService.delete(userId, memoryId);

        assertThat(record).isNotNull();
        verify(memoryRepository).delete(record);
        verify(embeddingRepository, never()).delete(any(MemoryEmbedding.class));
    }
}
