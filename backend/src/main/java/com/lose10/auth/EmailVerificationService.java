package com.lose10.auth;

import com.lose10.auth.security.TokenHasher;
import com.lose10.platform.error.ApiException;
import com.lose10.platform.error.ErrorCode;
import com.lose10.platform.notification.EmailSender;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class EmailVerificationService {

    private static final Duration TOKEN_TTL = Duration.ofHours(24);

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final TokenHasher hasher;
    private final EmailSender emailSender;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            UserRepository userRepository,
            TokenHasher hasher,
            EmailSender emailSender
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.hasher = hasher;
        this.emailSender = emailSender;
    }

    public void sendVerificationEmail(User user) {
        String raw = hasher.generateRawToken();
        tokenRepository.save(new EmailVerificationToken(user.getId(), hasher.hash(raw), Instant.now().plus(TOKEN_TTL)));
        emailSender.send(user.getEmail(), "Verify your LOSE10 account", "Your verification token: " + raw);
    }

    @Transactional
    public void verify(String rawToken) {
        String hash = hasher.hash(rawToken);
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash)
                .filter(EmailVerificationToken::isUsable)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED, HttpStatus.BAD_REQUEST, "Invalid or expired verification token"));

        token.markUsed();

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.markEmailVerified(Instant.now());
    }
}
