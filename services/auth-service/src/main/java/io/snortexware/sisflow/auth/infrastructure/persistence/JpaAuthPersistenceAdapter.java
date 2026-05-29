package io.snortexware.sisflow.auth.infrastructure.persistence;

import io.snortexware.sisflow.auth.application.AuthPersistence;
import io.snortexware.sisflow.auth.domain.model.EmailConfirmationToken;
import io.snortexware.sisflow.auth.domain.model.LoginAttempt;
import io.snortexware.sisflow.auth.domain.model.PasswordResetToken;
import io.snortexware.sisflow.auth.domain.model.RefreshToken;
import io.snortexware.sisflow.auth.domain.model.Tenant;
import io.snortexware.sisflow.auth.domain.model.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaAuthPersistenceAdapter implements AuthPersistence {

    private final SpringDataUserAccountRepository userAccountRepository;
    private final SpringDataRefreshTokenRepository refreshTokenRepository;
    private final SpringDataEmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final SpringDataPasswordResetTokenRepository passwordResetTokenRepository;
    private final SpringDataLoginAttemptRepository loginAttemptRepository;
    private final SpringDataUserRoleRepository userRoleRepository;
    private final SpringDataTenantRepository tenantRepository;

    @Override
    public Optional<UserAccount> findUserByEmailWithTenant(String email) {
        return userAccountRepository.findByEmailWithTenant(email);
    }

    @Override
    public Optional<UserAccount> findUserByIdWithTenant(UUID userId) {
        return userAccountRepository.findByIdWithTenant(userId);
    }

    @Override
    public Optional<UserAccount> findUserByEmail(String email) {
        return userAccountRepository.findByEmail(email);
    }

    @Override
    public UserAccount saveUser(UserAccount userAccount) {
        return userAccountRepository.save(userAccount);
    }

    @Override
    public boolean hasSystemAdminRole(UUID userId) {
        return userRoleRepository.hasSystemAdminRole(userId);
    }

    @Override
    public Optional<RefreshToken> findRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    public RefreshToken saveRefreshToken(RefreshToken refreshToken) {
        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void deleteRefreshToken(RefreshToken refreshToken) {
        refreshTokenRepository.delete(refreshToken);
    }

    @Override
    public Optional<EmailConfirmationToken> findEmailConfirmationToken(String token) {
        return emailConfirmationTokenRepository.findByToken(token);
    }

    @Override
    public EmailConfirmationToken saveEmailConfirmationToken(EmailConfirmationToken token) {
        return emailConfirmationTokenRepository.save(token);
    }

    @Override
    public void deleteEmailConfirmationTokensByUserId(UUID userId) {
        emailConfirmationTokenRepository.deleteByUserId(userId);
    }

    @Override
    public Optional<PasswordResetToken> findPasswordResetToken(String token) {
        return passwordResetTokenRepository.findByToken(token);
    }

    @Override
    public PasswordResetToken savePasswordResetToken(PasswordResetToken token) {
        return passwordResetTokenRepository.save(token);
    }

    @Override
    public void deletePasswordResetTokensByUserId(UUID userId) {
        passwordResetTokenRepository.deleteByUserId(userId);
    }

    @Override
    public long countFailedAttemptsSince(String email, OffsetDateTime since) {
        return loginAttemptRepository.countFailedAttemptsSince(email, since);
    }

    @Override
    public long countFailedAttemptsByIpSince(String ipAddress, OffsetDateTime since) {
        return loginAttemptRepository.countFailedAttemptsByIpSince(ipAddress, since);
    }

    @Override
    public LoginAttempt saveLoginAttempt(LoginAttempt loginAttempt) {
        return loginAttemptRepository.save(loginAttempt);
    }

    @Override
    public void deleteLoginAttemptsBefore(OffsetDateTime cutoff) {
        loginAttemptRepository.deleteByAttemptedAtBefore(cutoff);
    }

    @Override
    public Optional<Tenant> findTenantByDomain(String domain) {
        return tenantRepository.findByDomain(domain);
    }
}
