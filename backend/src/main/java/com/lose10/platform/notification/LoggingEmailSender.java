package com.lose10.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default EmailSender until SES wiring lands (Phase 11 / L10-1104). Logs
 * instead of sending so local/staging development isn't blocked on email
 * infrastructure — never used in production (swap the bean at that point).
 */
@Service
public class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void send(String toEmail, String subject, String body) {
        log.info("[EMAIL STUB] to={} subject={} body={}", toEmail, subject, body);
    }
}
