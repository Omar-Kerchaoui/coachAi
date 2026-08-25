package com.lose10.whatsapp;

import com.lose10.platform.error.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class WhatsAppAccountService {

    private static final String OPT_IN_TEMPLATE = "lose10_opt_in";

    private final WhatsAppAccountRepository accountRepository;
    private final WhatsAppProvider whatsAppProvider;

    public WhatsAppAccountService(WhatsAppAccountRepository accountRepository, WhatsAppProvider whatsAppProvider) {
        this.accountRepository = accountRepository;
        this.whatsAppProvider = whatsAppProvider;
    }

    /**
     * Sends the opt-in template; status only moves PENDING -> CONNECTED
     * once the user actually responds (docs/WHATSAPP.md §3), handled in
     * {@link WhatsAppWebhookService} identity resolution.
     */
    @Transactional
    public WhatsAppAccount connect(UUID userId, String phoneE164) {
        WhatsAppAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> new WhatsAppAccount(userId, phoneE164));
        accountRepository.save(account);

        WhatsAppProvider.SendResult result = whatsAppProvider.sendTemplate(phoneE164, OPT_IN_TEMPLATE, Map.of());
        if (!result.success()) {
            throw ApiException.businessRule("Failed to send WhatsApp opt-in message: " + result.errorMessage());
        }
        return account;
    }

    @Transactional
    public void disconnect(UUID userId) {
        accountRepository.findByUserId(userId).ifPresent(WhatsAppAccount::optOut);
    }

    @Transactional(readOnly = true)
    public Optional<WhatsAppAccount> findByPhone(String phoneE164) {
        return accountRepository.findByPhoneE164(phoneE164);
    }

    @Transactional
    public void handleOptOutKeyword(String phoneE164) {
        accountRepository.findByPhoneE164(phoneE164).ifPresent(WhatsAppAccount::optOut);
    }

    @Transactional
    public void confirmOptInIfPending(WhatsAppAccount account, String waId) {
        if (account.getStatus() == WhatsAppAccountStatus.PENDING) {
            account.confirmOptIn(waId);
        }
    }
}
