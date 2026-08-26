package com.lose10.whatsapp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WhatsAppAccountRepository extends JpaRepository<WhatsAppAccount, UUID> {
    Optional<WhatsAppAccount> findByUserId(UUID userId);

    Optional<WhatsAppAccount> findByPhoneE164(String phoneE164);

    void deleteByUserId(UUID userId);
}
