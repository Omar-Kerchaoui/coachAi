package com.lose10.admin;

import com.lose10.admin.dto.SendManualMessageRequest;
import com.lose10.conversation.ConversationService;
import com.lose10.platform.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/conversations")
public class AdminConversationController {

    private final ConversationService conversationService;
    private final AdminCoachingOverrideService coachingOverrideService;

    public AdminConversationController(ConversationService conversationService, AdminCoachingOverrideService coachingOverrideService) {
        this.conversationService = conversationService;
        this.coachingOverrideService = coachingOverrideService;
    }

    @PostMapping("/{threadId}/messages")
    public ResponseEntity<Void> sendManualMessage(@PathVariable UUID threadId, @Valid @RequestBody SendManualMessageRequest request) {
        UUID userId = conversationService.userIdForThread(threadId);
        coachingOverrideService.sendManualMessage(userId, CurrentUser.id(), request.message());
        return ResponseEntity.status(201).build();
    }
}
