package io.snortexware.sisflow.auth.application;

import io.snortexware.sisflow.auth.domain.model.EmailConfirmationToken;
import io.snortexware.sisflow.auth.domain.model.LoginAttempt;
import io.snortexware.sisflow.auth.domain.model.PasswordResetToken;
import io.snortexware.sisflow.auth.domain.model.RefreshToken;
import io.snortexware.sisflow.auth.domain.model.Tenant;
import io.snortexware.sisflow.auth.domain.model.UserAccount;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface AuthPersistence {
    Optional<UserAccount> findUserByEmailWithTenant(String email);
    Optional<UserAccount> findUserByIdWithTenant(UUID userId);
    Optional<UserAccount> findUserByEmail(String email);
    UserAccount saveUser(UserAccount userAccount);
    boolean hasSystemAdminRole(UUID userId);

    Optional<RefreshToken> findRefreshToken(String token);
    RefreshToken saveRefreshToken(RefreshToken refreshToken);
    void deleteRefreshToken(RefreshToken refreshToken);

    Optional<EmailConfirmationToken> findEmailConfirmationToken(String token);
    EmailConfirmationToken saveEmailConfirmationToken(EmailConfirmationToken token);
    void deleteEmailConfirmationTokensByUserId(UUID userId);

    Optional<PasswordResetToken> findPasswordResetToken(String token);
    PasswordResetToken savePasswordResetToken(PasswordResetToken token);
    void deletePasswordResetTokensByUserId(UUID userId);

    long countFailedAttemptsSince(String email, OffsetDateTime since);
    long countFailedAttemptsByIpSince(String ipAddress, OffsetDateTime since);
    LoginAttempt saveLoginAttempt(LoginAttempt loginAttempt);
    void deleteLoginAttemptsBefore(OffsetDateTime cutoff);

    Optional<Tenant> findTenantByDomain(String domain);
}
