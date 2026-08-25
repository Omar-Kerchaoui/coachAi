package com.lose10.admin;

import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminNoteService {

    private final AdminNoteRepository repository;
    private final AuditLogService auditLogService;

    public AdminNoteService(AdminNoteRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AdminNote addNote(UUID userId, UUID adminId, String note) {
        AdminNote saved = repository.save(new AdminNote(userId, adminId, note));
        auditLogService.record(adminId, AuditLog.ActorType.ADMIN, "ADMIN_NOTE_ADDED", "User", userId);
        return saved;
    }
}
