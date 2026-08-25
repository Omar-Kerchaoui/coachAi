package com.lose10.auth;

import com.lose10.auth.dto.TokenPairResponse;
import com.lose10.auth.dto.UserSummary;
import com.lose10.auth.event.UserRegisteredEvent;
import com.lose10.auth.security.JwtService;
import com.lose10.auth.security.RefreshTokenService;
import com.lose10.platform.audit.AuditLog;
import com.lose10.platform.audit.AuditLogService;
import com.lose10.platform.error.ApiException;
import com.lose10.platform.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService implements AccountService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final Duration refreshTokenTtl;

    public AuthService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            EmailVerificationService emailVerificationService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher,
            @Value("${lose10.auth.refresh-token-ttl-days}") long refreshTokenTtlDays
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.emailVerificationService = emailVerificationService;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.refreshTokenTtl = Duration.ofDays(refreshTokenTtlDays);
    }

    @Transactional
    public User register(String email, String rawPassword) {
        String normalizedEmail = email.toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED, HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = new User(normalizedEmail, passwordEncoder.encode(rawPassword));
        user = userRepository.save(user);
        userRoleRepository.save(new UserRole(user.getId(), Role.USER));
        emailVerificationService.sendVerificationEmail(user);
        eventPublisher.publishEvent(new UserRegisteredEvent(user.getId()));
        return user;
    }

    @Transactional
    public TokenPairResponse login(String email, String rawPassword, String ip, String userAgent) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash()) || !user.isActive()) {
            // Same error for wrong password and inactive/deleted account — never leak account state.
            throw new BadCredentialsException("Invalid credentials");
        }

        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().name())
                .toList();

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);
        RefreshTokenService.IssuedToken refreshToken = refreshTokenService.issueNewFamily(user.getId());
        sessionRepository.save(new Session(user.getId(), ip, userAgent, Instant.now().plus(refreshTokenTtl)));

        return new TokenPairResponse(accessToken, refreshToken.rawToken(), new UserSummary(user.getId(), user.getEmail()));
    }

    @Transactional
    public TokenPairResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository.findById(result.userId())
                .orElseThrow(() -> ApiException.notFound("User not found"));
        List<String> roles = userRoleRepository.findByUserId(user.getId()).stream()
                .map(ur -> ur.getRole().name())
                .toList();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roles);
        return new TokenPairResponse(accessToken, result.issuedToken().rawToken(), new UserSummary(user.getId(), user.getEmail()));
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    /**
     * Soft-delete only (L10-210). The full cross-domain cascade purge is
     * implemented in E10 (L10-1004) once every domain that can hold this
     * user's data exists.
     */
    @Override
    @Transactional
    public void deleteAccount(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.softDelete();
        refreshTokenService.revokeAllForUser(userId);
        auditLogService.record(userId, AuditLog.ActorType.USER, "USER_DELETED", "User", userId);
    }
}
