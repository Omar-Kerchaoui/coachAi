package com.lose10.whatsapp;

import com.lose10.conversation.MessageDirection;
import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_messages")
public class WhatsAppMessage extends MutableEntity {

    @Column(name = "message_id")
    private UUID messageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageDirection direction;

    @Column(name = "wa_message_id", length = 100)
    private String waMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WhatsAppMessageStatus status = WhatsAppMessageStatus.SENT;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private Map<String, Object> rawPayload;

    protected WhatsAppMessage() {
    }

    public WhatsAppMessage(UUID messageId, MessageDirection direction, String waMessageId, WhatsAppMessageStatus status, Map<String, Object> rawPayload) {
        this.messageId = messageId;
        this.direction = direction;
        this.waMessageId = waMessageId;
        this.status = status;
        this.rawPayload = rawPayload;
    }

    public void updateStatus(WhatsAppMessageStatus status) {
        this.status = status;
    }

    public String getWaMessageId() {
        return waMessageId;
    }

    public WhatsAppMessageStatus getStatus() {
        return status;
    }
}
