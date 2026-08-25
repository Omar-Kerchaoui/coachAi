package com.lose10.auth;

import com.lose10.auth.dto.LoginRequest;
import com.lose10.auth.dto.PasswordResetConfirm;
import com.lose10.auth.dto.PasswordResetRequest;
import com.lose10.auth.dto.RefreshRequest;
import com.lose10.auth.dto.RegisterRequest;
import com.lose10.auth.dto.TokenPairResponse;
import com.lose10.auth.dto.VerifyEmailRequest;
import com.lose10.platform.ratelimit.RateLimitExceededException;
import com.lose10.platform.ratelimit.RateLimiterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Duration PUBLIC_WINDOW = Duration.ofMinutes(15);
    private static final int PUBLIC_LIMIT = 20;

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;
    private final RateLimiterService rateLimiter;

    public AuthController(
            AuthService authService,
            EmailVerificationService emailVerificationService,
            PasswordResetService passwordResetService,
            RateLimiterService rateLimiter
    ) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        enforceRateLimit("register", httpRequest);
        authService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        enforceRateLimit("login:" + request.email().toLowerCase(), httpRequest);
        TokenPairResponse tokens = authService.login(
                request.email(), request.password(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verify(request.token());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request, HttpServletRequest httpRequest) {
        enforceRateLimit("password-reset:" + request.email().toLowerCase(), httpRequest);
        passwordResetService.requestReset(request.email());
        // Always 200 regardless of whether the email exists — no user enumeration (docs/SECURITY.md §1).
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirm request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private void enforceRateLimit(String key, HttpServletRequest request) {
        String rateLimitKey = key + ":" + request.getRemoteAddr();
        if (!rateLimiter.tryConsume(rateLimitKey, PUBLIC_LIMIT, PUBLIC_WINDOW)) {
            throw new RateLimitExceededException();
        }
    }
}
