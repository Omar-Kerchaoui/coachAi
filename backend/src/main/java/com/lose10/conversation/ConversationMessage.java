package com.lose10.conversation;

import com.lose10.platform.persistence.BaseEntity;
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
@Table(name = "messages")
public class ConversationMessage extends BaseEntity {

    @Column(name = "thread_id", nullable = false)
    private UUID threadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageSender sender;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ai_metadata", columnDefinition = "jsonb")
    private Map<String, Object> aiMetadata;

    @Column(name = "whatsapp_message_id", length = 100)
    private String whatsappMessageId;

    protected ConversationMessage() {
    }

    public ConversationMessage(UUID threadId, MessageDirection direction, MessageSender sender, String content) {
        this.threadId = threadId;
        this.direction = direction;
        this.sender = sender;
        this.content = content;
    }

    public UUID getThreadId() {
        return threadId;
    }

    public MessageDirection getDirection() {
        return direction;
    }

    public MessageSender getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }
}
