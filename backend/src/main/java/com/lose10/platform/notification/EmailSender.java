package com.lose10.platform.notification;

/**
 * Abstraction over transactional email delivery (docs/DEPLOYMENT.md — SES in
 * production). WhatsApp remains the primary coaching channel; email is only
 * ever used for account/billing transactional messages, never coaching.
 */
public interface EmailSender {
    void send(String toEmail, String subject, String body);
}
