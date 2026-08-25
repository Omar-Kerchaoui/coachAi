package com.lose10.memory;

import com.lose10.memory.dto.MemoryResponse;
import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import com.lose10.platform.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MemoryService {

    private final MemoryRecordRepository memoryRepository;
    private final MemoryEmbeddingRepository embeddingRepository;
    private final AuditLogService auditLogService;

    public MemoryService(
            MemoryRecordRepository memoryRepository,
            MemoryEmbeddingRepository embeddingRepository,
            AuditLogService auditLogService
    ) {
        this.memoryRepository = memoryRepository;
        this.embeddingRepository = embeddingRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<MemoryResponse> listOwn(UUID userId) {
        return memoryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(MemoryResponse::from).toList();
    }

    /**
     * Hard delete — structured row and embedding, audited via
     * {@code audit_logs} (docs/MEMORY.md §8: user-initiated memory deletion
     * is a privacy right, not a soft-delete). Not written to
     * {@code memory_events}: that table cascade-deletes with its parent
     * memory row, so a "DELETED" event there could never outlive the
     * delete it's supposed to record — audit_logs is the durable trail.
     */
    @Transactional
    public void delete(UUID userId, UUID memoryId) {
        MemoryRecord memory = memoryRepository.findByIdAndUserId(memoryId, userId)
                .orElseThrow(() -> ApiException.notFound("Memory not found"));

        embeddingRepository.findByMemoryId(memory.getId()).ifPresent(embeddingRepository::delete);
        memoryRepository.delete(memory);

        auditLogService.record(userId, AuditLog.ActorType.USER, "MEMORY_DELETED", "MemoryRecord", memoryId);
    }
}
