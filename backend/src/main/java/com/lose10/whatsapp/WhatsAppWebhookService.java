package com.lose10.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lose10.ai.CoachingTurnService;
import com.lose10.conversation.MessageDirection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the inbound flow in docs/WHATSAPP.md §1: idempotent event
 * recording, identity resolution, opt-in/opt-out handling, then handing off
 * to the same {@link CoachingTurnService} path the internal test endpoint
 * uses. Every step that can be skipped safely (unknown number, already-
 * processed event, opted-out account) returns without throwing — Meta
 * expects 200 OK regardless, per docs/WHATSAPP.md §1.
 *
 * <p>Deliberately NOT wrapped in one outer {@code @Transactional}: this
 * chain calls into {@code CoachingTurnService}, which runs the full AI
 * orchestrator (a slow external call). Wrapping that in a long-lived DB
 * transaction is bad practice on its own, but it's also actively broken —
 * {@code ContextBuilder} gracefully catches "not found" exceptions from
 * inner {@code @Transactional} reads (e.g. no active goal yet), but
 * Spring's AOP proxy marks the *ambient* transaction rollback-only the
 * instant such an exception leaves the inner method, regardless of the
 * catch one frame up. Under one shared transaction that poisons the whole
 * request, producing UnexpectedRollbackException at commit time even
 * though every step "succeeded." See {@link WhatsAppWebhookEventRecorder}
 * for where the actually-needed short transactions live. Caught via a live
 * webhook test, not by inspection.
 */
@Service
public class WhatsAppWebhookService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookService.class);
    private static final String STOP_KEYWORD = "STOP";

    private final WhatsAppWebhookEventRecorder eventRecorder;
    private final WhatsAppAccountService accountService;
    private final WhatsAppMessageRepository whatsAppMessageRepository;
    private final CoachingTurnService coachingTurnService;
    private final WhatsAppProvider whatsAppProvider;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookService(
            WhatsAppWebhookEventRecorder eventRecorder,
            WhatsAppAccountService accountService,
            WhatsAppMessageRepository whatsAppMessageRepository,
            CoachingTurnService coachingTurnService,
            WhatsAppProvider whatsAppProvider,
            ObjectMapper objectMapper
    ) {
        this.eventRecorder = eventRecorder;
        this.accountService = accountService;
        this.whatsAppMessageRepository = whatsAppMessageRepository;
        this.coachingTurnService = coachingTurnService;
        this.whatsAppProvider = whatsAppProvider;
        this.objectMapper = objectMapper;
    }

    public void process(JsonNode payload) {
        String eventId = deriveEventId(payload);
        if (eventRecorder.isAlreadyProcessed(eventId)) {
            log.debug("Webhook event {} already processed — skipping", eventId);
            return;
        }
        UUID eventRecordId = eventRecorder.recordReceived(eventId, toMap(payload));

        boolean success = true;
        try {
            for (JsonNode entry : payload.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    JsonNode value = change.path("value");
                    for (JsonNode message : value.path("messages")) {
                        handleInboundMessage(message);
                    }
                    for (JsonNode status : value.path("statuses")) {
                        handleStatusUpdate(status);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process WhatsApp webhook event {}", eventId, e);
            success = false;
        }
        eventRecorder.markOutcome(eventRecordId, success);
    }

    private void handleInboundMessage(JsonNode message) {
        String from = message.path("from").asText(null);
        String waMessageId = message.path("id").asText(null);
        String text = message.path("text").path("body").asText(null);
        if (from == null || text == null) {
            return; // non-text message type (image/audio/etc.) — media handling is a future extension
        }

        Optional<WhatsAppAccount> maybeAccount = accountService.findByPhone(from);
        if (maybeAccount.isEmpty()) {
            log.info("Inbound WhatsApp message from unknown number — ignoring");
            return;
        }
        WhatsAppAccount account = maybeAccount.get();

        if (STOP_KEYWORD.equalsIgnoreCase(text.trim())) {
            accountService.handleOptOutKeyword(from);
            return;
        }

        accountService.confirmOptInIfPending(account, from);
        if (!account.canReceiveMessages()) {
            log.info("Ignoring inbound message from opted-out account {}", account.getUserId());
            return;
        }

        Optional<com.lose10.conversation.ConversationMessage> outbound = coachingTurnService.handleInboundMessage(account.getUserId(), text);
        recordInboundWhatsAppMessage(waMessageId, account.getUserId());

        outbound.ifPresent(outboundMessage -> sendAndLink(account, outboundMessage));
    }

    /**
     * {@code CoachingTurnService} already persisted this message (it's the
     * single point of record for outbound coaching replies — see its
     * Javadoc) — this only sends it and attaches WhatsApp delivery
     * metadata, never records the text a second time.
     */
    private void sendAndLink(WhatsAppAccount account, com.lose10.conversation.ConversationMessage outboundMessage) {
        WhatsAppProvider.SendResult result = whatsAppProvider.sendText(account.getPhoneE164(), outboundMessage.getContent());
        WhatsAppMessageStatus status = result.success() ? WhatsAppMessageStatus.SENT : WhatsAppMessageStatus.FAILED;
        whatsAppMessageRepository.save(new WhatsAppMessage(outboundMessage.getId(), MessageDirection.OUTBOUND, result.waMessageId(), status, Map.of()));
    }

    private void recordInboundWhatsAppMessage(String waMessageId, java.util.UUID userId) {
        // The ConversationMessage itself is already recorded by
        // CoachingTurnService.handleInboundMessage — this just links the
        // WhatsApp-specific delivery metadata (wa_message_id) alongside it.
        whatsAppMessageRepository.save(new WhatsAppMessage(null, MessageDirection.INBOUND, waMessageId, WhatsAppMessageStatus.DELIVERED, Map.of()));
    }

    private void handleStatusUpdate(JsonNode status) {
        String waMessageId = status.path("id").asText(null);
        String statusValue = status.path("status").asText(null);
        if (waMessageId == null || statusValue == null) {
            return;
        }
        whatsAppMessageRepository.findByWaMessageId(waMessageId).ifPresent(msg -> {
            try {
                msg.updateStatus(WhatsAppMessageStatus.valueOf(statusValue.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // "sent"/"delivered"/"read"/"failed" are the ones we model; anything else is ignored
            }
        });
    }

    private String deriveEventId(JsonNode payload) {
        for (JsonNode entry : payload.path("entry")) {
            for (JsonNode change : entry.path("changes")) {
                JsonNode value = change.path("value");
                for (JsonNode message : value.path("messages")) {
                    if (message.has("id")) {
                        return "msg:" + message.get("id").asText();
                    }
                }
                for (JsonNode status : value.path("statuses")) {
                    if (status.has("id") && status.has("status")) {
                        return "status:" + status.get("id").asText() + ":" + status.get("status").asText();
                    }
                }
            }
        }
        return "hash:" + sha256(payload.toString());
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(JsonNode payload) {
        return objectMapper.convertValue(payload, Map.class);
    }
}
