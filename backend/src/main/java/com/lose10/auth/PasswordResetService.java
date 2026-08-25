package com.lose10.auth;

import com.lose10.auth.security.RefreshTokenService;
import com.lose10.auth.security.TokenHasher;
import com.lose10.platform.error.ApiException;
import com.lose10.platform.error.ErrorCode;
import com.lose10.platform.notification.EmailSender;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * docs/SECURITY.md §1: requesting a reset never reveals whether the email
 * exists — always succeeds from the caller's point of view. Confirming a
 * reset revokes every existing refresh token for the user, so a stolen
 * session can't survive a password change.
 */
@Service
public class PasswordResetService {

    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final TokenHasher hasher;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            TokenHasher hasher,
            EmailSender emailSender,
            PasswordEncoder passwordEncoder,
            RefreshTokenService refreshTokenService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.hasher = hasher;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenService = refreshTokenService;
    }

    public void requestReset(String email) {
        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            String raw = hasher.generateRawToken();
            tokenRepository.save(new PasswordResetToken(user.getId(), hasher.hash(raw), Instant.now().plus(TOKEN_TTL)));
            emailSender.send(user.getEmail(), "Reset your LOSE10 password", "Your reset token: " + raw);
        });
        // Intentionally no branch on whether the user existed — same response either way.
    }

    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        String hash = hasher.hash(rawToken);
        PasswordResetToken token = tokenRepository.findByTokenHash(hash)
                .filter(PasswordResetToken::isUsable)
                .orElseThrow(() -> new ApiException(ErrorCode.TOKEN_EXPIRED, HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));

        token.markUsed();

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.changePasswordHash(passwordEncoder.encode(newPassword));

        refreshTokenService.revokeAllForUser(user.getId());
    }
}
