package com.lose10.conversation;

import com.lose10.platform.persistence.MutableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "conversation_threads")
public class ConversationThread extends MutableEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 20)
    private String channel = "WHATSAPP";

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    protected ConversationThread() {
    }

    public ConversationThread(UUID userId, String channel) {
        this.userId = userId;
        this.channel = channel;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getChannel() {
        return channel;
    }

    public String getStatus() {
        return status;
    }
}
