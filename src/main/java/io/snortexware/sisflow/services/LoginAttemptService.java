package io.snortexware.sisflow.services;

import io.snortexware.sisflow.entities.LoginAttempt;
import io.snortexware.sisflow.repositories.LoginAttemptRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
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
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final RateLimitService rateLimitService;

    @Value("${security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${security.login.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    @Value("${security.login.ip-max-attempts:50}")
    private int ipMaxAttempts;

    @Value("${security.login.rate-limit-capacity:30}")
    private int rateLimitCapacity;

    @Value("${security.login.rate-limit-window-minutes:1}")
    private int rateLimitWindowMinutes;

    public void validateLoginAttempt(String email, String ipAddress) {
        OffsetDateTime since = OffsetDateTime.now().minusMinutes(lockoutDurationMinutes);
        
        long failedAttempts = loginAttemptRepository.countFailedAttemptsSince(email, since);
        if (failedAttempts >= maxAttempts) {
            log.warn("Account locked due to too many failed attempts: {}", email);
            throw AppException.forbidden();
        }

        long ipAttempts = loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, since);
        if (ipAttempts >= ipMaxAttempts) {
            log.warn("IP blocked due to too many failed attempts: {}", ipAddress);
            throw AppException.forbidden();
        }

        var bucket = rateLimitService.consume(
                "login:" + ipAddress,
                rateLimitCapacity,
                Duration.ofMinutes(rateLimitWindowMinutes)
        );
        if (!bucket.allowed()) {
            log.warn("Rate limit exceeded for IP: {}", ipAddress);
            throw AppException.forbidden();
        }
    }

    @Transactional
    public void recordLoginAttempt(String email, String ipAddress, boolean successful) {
        loginAttemptRepository.save(LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .successful(successful)
                .attemptedAt(OffsetDateTime.now())
                .build());

        if (successful) {
            rateLimitService.clearBucket("login:" + ipAddress);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupOldAttempts() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(7);
        loginAttemptRepository.deleteByAttemptedAtBefore(cutoff);
        log.info("Cleaned up login attempts older than 7 days");
    }
}
