package com.lose10.conversation.event;

import com.lose10.conversation.MessageSender;

import java.util.UUID;

public record MessageRecordedEvent(UUID userId, UUID messageId, MessageSender sender, String content) {
}
