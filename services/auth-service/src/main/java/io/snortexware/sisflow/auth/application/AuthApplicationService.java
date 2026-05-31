package io.snortexware.sisflow.auth.application;

import io.snortexware.sisflow.auth.application.port.NotificationPort;
import io.snortexware.sisflow.auth.application.port.TenantResolutionPort;
import io.snortexware.sisflow.auth.application.port.TokenPort;
import io.snortexware.sisflow.auth.domain.model.EmailConfirmationToken;
import io.snortexware.sisflow.auth.domain.model.PasswordResetToken;
import io.snortexware.sisflow.auth.domain.model.RefreshToken;
import io.snortexware.sisflow.auth.domain.model.Tenant;
import io.snortexware.sisflow.auth.domain.model.UserAccount;
import io.snortexware.sisflow.auth.interfaces.http.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthApplicationService {

    private final AuthPersistence authPersistence;
    private final TokenPort tokenPort;
    private final NotificationPort notificationPort;
    private final TenantResolutionPort tenantResolutionPort;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptApplicationService loginAttemptApplicationService;

    @Value("${jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    @Value("${jwt.expiration-ms:3600000}")
    private long accessExpirationMs;

    @Value("${email.confirmation.expiration-hours:24}")
    private long confirmationExpirationHours;

    @Transactional
    public Map<String, Object> signIn(String email, String password, HttpServletRequest request) {
        String ipAddress = clientIp(request);
        loginAttemptApplicationService.validateLoginAttempt(email, ipAddress);

        UserAccount user = authPersistence.findUserByEmailWithTenant(email).orElseThrow(() -> {
            loginAttemptApplicationService.recordLoginAttempt(email, ipAddress, false);
            return ApiException.notFound();
        });

        var requestTenant = tenantResolutionPort.resolve(request);
        if (requestTenant.isPresent()) {
            if (user.getTenant() == null || !user.getTenant().getId().equals(requestTenant.get().getId())) {
                loginAttemptApplicationService.recordLoginAttempt(email, ipAddress, false);
                throw ApiException.notFound();
            }
        } else if (user.getTenant() != null || !authPersistence.hasSystemAdminRole(user.getId())) {
            loginAttemptApplicationService.recordLoginAttempt(email, ipAddress, false);
            throw ApiException.notFound();
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            loginAttemptApplicationService.recordLoginAttempt(email, ipAddress, false);
            throw ApiException.badRequest();
        }
        if (!user.isEmailConfirmed()) {
            loginAttemptApplicationService.recordLoginAttempt(email, ipAddress, false);
            throw ApiException.forbidden();
        }

        loginAttemptApplicationService.recordLoginAttempt(email, ipAddress, true);
        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        return issueTokens(user.getId(), tenantId);
    }

    @Transactional
    public Map<String, Object> signUp(String email, String password, String fullName) {
        if (authPersistence.findUserByEmail(email).isPresent()) {
            return Map.of("status", "pending_confirmation");
        }

        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setName(fullName);
        user.setRole(UserAccount.Role.client);
        user.setType(UserAccount.UserType.end_user);
        user.setEmailConfirmed(false);
        user.setCreatedAt(OffsetDateTime.now());
        authPersistence.saveUser(user);

        notificationPort.publishConfirmationEmail(email, issueConfirmationToken(user.getId()));
        return Map.of("status", "pending_confirmation");
    }

    @Transactional
    public Map<String, Object> confirmEmail(String token) {
        EmailConfirmationToken confirmationToken = authPersistence.findEmailConfirmationToken(token)
                .orElseThrow(ApiException::badRequest);

        if (confirmationToken.isUsed() || confirmationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw ApiException.badRequest();
        }

        UserAccount user = authPersistence.findUserByIdWithTenant(confirmationToken.getUserId())
                .orElseThrow(ApiException::notFound);

        user.setEmailConfirmed(true);
        authPersistence.saveUser(user);
        confirmationToken.setUsed(true);
        authPersistence.saveEmailConfirmationToken(confirmationToken);

        UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
        return issueTokens(user.getId(), tenantId);
    }

    @Transactional
    public void resendConfirmation(String email) {
        authPersistence.findUserByEmail(email).ifPresent(user -> {
            if (!user.isEmailConfirmed()) {
                authPersistence.deleteEmailConfirmationTokensByUserId(user.getId());
                notificationPort.publishConfirmationEmail(email, issueConfirmationToken(user.getId()));
            }
        });
    }

    @Transactional
    public void requestPasswordReset(String email) {
        authPersistence.findUserByEmail(email).ifPresent(user -> {
            authPersistence.deletePasswordResetTokensByUserId(user.getId());
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setToken(UUID.randomUUID().toString());
            token.setExpiresAt(OffsetDateTime.now().plusHours(1));
            token.setUsed(false);
            token.setCreatedAt(OffsetDateTime.now());
            authPersistence.savePasswordResetToken(token);
            notificationPort.publishPasswordResetEmail(email, token.getToken());
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken passwordResetToken = authPersistence.findPasswordResetToken(token)
                .orElseThrow(ApiException::badRequest);

        if (passwordResetToken.isUsed() || passwordResetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw ApiException.badRequest();
        }

        UserAccount user = authPersistence.findUserByIdWithTenant(passwordResetToken.getUserId())
                .orElseThrow(ApiException::notFound);

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        authPersistence.saveUser(user);
        passwordResetToken.setUsed(true);
        authPersistence.savePasswordResetToken(passwordResetToken);
    }

    @Transactional
    public Map<String, Object> refresh(String rawRefreshToken) {
        RefreshToken refreshToken = authPersistence.findRefreshToken(rawRefreshToken)
                .orElseThrow(ApiException::unauthorized);

        if (refreshToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            authPersistence.deleteRefreshToken(refreshToken);
            throw ApiException.unauthorized();
        }

        authPersistence.deleteRefreshToken(refreshToken);
        UUID tenantId = authPersistence.findUserByIdWithTenant(refreshToken.getUserId())
                .map(user -> user.getTenant() != null ? user.getTenant().getId() : null)
                .orElse(null);
        return issueTokens(refreshToken.getUserId(), tenantId);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        authPersistence.findRefreshToken(rawRefreshToken)
                .ifPresent(authPersistence::deleteRefreshToken);
    }

    private String issueConfirmationToken(UUID userId) {
        EmailConfirmationToken token = new EmailConfirmationToken();
        token.setUserId(userId);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(OffsetDateTime.now().plusHours(confirmationExpirationHours));
        token.setUsed(false);
        token.setCreatedAt(OffsetDateTime.now());
        authPersistence.saveEmailConfirmationToken(token);
        return token.getToken();
    }

    private Map<String, Object> issueTokens(UUID userId, UUID tenantId) {
        String accessToken = tokenPort.generateAccessToken(userId, tenantId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays));
        refreshToken.setCreatedAt(OffsetDateTime.now());
        authPersistence.saveRefreshToken(refreshToken);

        return Map.of(
                "accessToken", accessToken,
                "refreshToken", refreshToken.getToken(),
                "expiresIn", 3600
        );
    }

    public Duration accessTokenLifetime() {
        return Duration.ofMillis(accessExpirationMs);
    }

    public Duration refreshTokenLifetime() {
        return Duration.ofDays(refreshExpirationDays);
    }

    private String clientIp(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
