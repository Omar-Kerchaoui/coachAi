package com.lose10.admin;

import com.lose10.platform.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "admin_notes")
public class AdminNote extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(nullable = false, columnDefinition = "text")
    private String note;

    protected AdminNote() {
    }

    public AdminNote(UUID userId, UUID adminId, String note) {
        this.userId = userId;
        this.adminId = adminId;
        this.note = note;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public String getNote() {
        return note;
    }
}
