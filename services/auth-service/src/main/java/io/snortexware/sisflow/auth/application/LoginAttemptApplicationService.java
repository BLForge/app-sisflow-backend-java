package io.snortexware.sisflow.auth.application;

import io.snortexware.sisflow.auth.application.port.RateLimitPort;
import io.snortexware.sisflow.auth.domain.model.LoginAttempt;
import io.snortexware.sisflow.auth.interfaces.http.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptApplicationService {

    private final AuthPersistence authPersistence;
    private final RateLimitPort rateLimitPort;

    @Value("${security.login.max-attempts:10}")
    private int maxAttempts;

    @Value("${security.login.ip-max-attempts:50}")
    private int ipMaxAttempts;

    @Value("${security.login.rate-limit-capacity:30}")
    private int rateLimitCapacity;

    @Value("${security.login.rate-limit-window-minutes:1}")
    private int rateLimitWindowMinutes;

    @Value("${security.login.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    public void validateLoginAttempt(String email, String ipAddress) {
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(lockoutDurationMinutes);

        if (authPersistence.countFailedAttemptsSince(email, since) >= maxAttempts) {
            log.warn("Account locked due to too many failed attempts: {}", email);
            throw ApiException.forbidden();
        }

        if (authPersistence.countFailedAttemptsByIpSince(ipAddress, since) >= ipMaxAttempts) {
            log.warn("IP blocked due to too many failed attempts: {}", ipAddress);
            throw ApiException.forbidden();
        }

        if (!rateLimitPort.tryConsume("login:" + ipAddress, rateLimitCapacity, Duration.ofMinutes(rateLimitWindowMinutes))) {
            log.warn("Login rate limit exceeded for IP: {}", ipAddress);
            throw ApiException.forbidden();
        }
    }

    @Transactional
    public void recordLoginAttempt(String email, String ipAddress, boolean successful) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.setEmail(email);
        attempt.setIpAddress(ipAddress);
        attempt.setSuccessful(successful);
        attempt.setAttemptedAt(OffsetDateTime.now());
        authPersistence.saveLoginAttempt(attempt);

        if (successful) {
            rateLimitPort.clear("login:" + ipAddress);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupOldAttempts() {
        authPersistence.deleteLoginAttemptsBefore(OffsetDateTime.now().minusDays(7));
    }
}
