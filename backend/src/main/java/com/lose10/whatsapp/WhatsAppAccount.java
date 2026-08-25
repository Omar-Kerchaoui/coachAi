package com.lose10.whatsapp;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_accounts")
public class WhatsAppAccount extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "phone_e164", nullable = false, length = 20)
    private String phoneE164;

    @Column(name = "wa_id", length = 50)
    private String waId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WhatsAppAccountStatus status = WhatsAppAccountStatus.PENDING;

    @Column(name = "opted_in_at")
    private Instant optedInAt;

    @Column(name = "opted_out_at")
    private Instant optedOutAt;

    protected WhatsAppAccount() {
    }

    public WhatsAppAccount(UUID userId, String phoneE164) {
        this.userId = userId;
        this.phoneE164 = phoneE164;
    }

    public void confirmOptIn(String waId) {
        this.waId = waId;
        this.status = WhatsAppAccountStatus.CONNECTED;
        this.optedInAt = Instant.now();
        this.optedOutAt = null;
    }

    public void optOut() {
        this.status = WhatsAppAccountStatus.OPTED_OUT;
        this.optedOutAt = Instant.now();
    }

    public boolean canReceiveMessages() {
        return status == WhatsAppAccountStatus.CONNECTED;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPhoneE164() {
        return phoneE164;
    }

    public String getWaId() {
        return waId;
    }

    public WhatsAppAccountStatus getStatus() {
        return status;
    }
}
