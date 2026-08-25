package com.lose10.whatsapp;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Read-only access to {@link WhatsAppMessage} for the WhatsApp cost
 * dashboard (docs/backlog L10-912) — same rationale as
 * {@code AiUsageQueryService}.
 */
@Service
public class WhatsAppUsageQueryService {

    private final WhatsAppMessageRepository repository;

    public WhatsAppUsageQueryService(WhatsAppMessageRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<WhatsAppMessage> between(Instant from, Instant to) {
        return repository.findByCreatedAtBetween(from, to);
    }
}
