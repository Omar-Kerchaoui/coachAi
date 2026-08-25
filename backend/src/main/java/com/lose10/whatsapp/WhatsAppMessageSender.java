package com.lose10.whatsapp;

import com.lose10.conversation.MessageDirection;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The public entry point other modules (admin, for the human-coach manual
 * send — docs/backlog L10-907) use to deliver a message over WhatsApp
 * without reaching into this module's repositories directly
 * (docs/ARCHITECTURE.md §4). Sending logic here deliberately does not
 * duplicate {@code WhatsAppWebhookService.sendAndLink} — that path only
 * exists mid-inbound-webhook-processing, whereas this one starts from an
 * arbitrary caller with just a userId.
 */
@Service
public class WhatsAppMessageSender {

    private final WhatsAppAccountService accountService;
    private final WhatsAppProvider whatsAppProvider;
    private final WhatsAppMessageRepository whatsAppMessageRepository;

    public WhatsAppMessageSender(WhatsAppAccountService accountService, WhatsAppProvider whatsAppProvider, WhatsAppMessageRepository whatsAppMessageRepository) {
        this.accountService = accountService;
        this.whatsAppProvider = whatsAppProvider;
        this.whatsAppMessageRepository = whatsAppMessageRepository;
    }

    @Transactional
    public boolean send(UUID userId, UUID conversationMessageId, String text) {
        Optional<WhatsAppAccount> account = accountService.findByUserId(userId);
        if (account.isEmpty() || !account.get().canReceiveMessages()) {
            return false;
        }
        WhatsAppProvider.SendResult result = whatsAppProvider.sendText(account.get().getPhoneE164(), text);
        WhatsAppMessageStatus status = result.success() ? WhatsAppMessageStatus.SENT : WhatsAppMessageStatus.FAILED;
        whatsAppMessageRepository.save(new WhatsAppMessage(conversationMessageId, MessageDirection.OUTBOUND, result.waMessageId(), status, Map.of()));
        return result.success();
    }
}
