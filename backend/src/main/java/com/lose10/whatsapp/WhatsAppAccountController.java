package com.lose10.whatsapp;

import com.lose10.platform.security.CurrentUser;
import com.lose10.whatsapp.dto.ConnectWhatsAppRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsAppAccountController {

    private final WhatsAppAccountService accountService;

    public WhatsAppAccountController(WhatsAppAccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/connect")
    public ResponseEntity<Void> connect(@Valid @RequestBody ConnectWhatsAppRequest request) {
        // Stored without the leading "+" to match the digit-only format
        // Meta sends in the webhook "from" field (docs/WHATSAPP.md).
        String normalizedPhone = request.phoneE164().substring(1);
        accountService.connect(CurrentUser.id(), normalizedPhone);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect() {
        accountService.disconnect(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
