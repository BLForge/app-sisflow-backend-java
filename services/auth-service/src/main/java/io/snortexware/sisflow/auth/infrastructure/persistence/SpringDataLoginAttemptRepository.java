package io.snortexware.sisflow.auth.infrastructure.persistence;

import io.snortexware.sisflow.auth.domain.model.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.UUID;

interface SpringDataLoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    @Query("select count(la) from LoginAttempt la where la.email = :email and la.successful = false and la.attemptedAt > :since")
    long countFailedAttemptsSince(String email, OffsetDateTime since);

    @Query("select count(la) from LoginAttempt la where la.ipAddress = :ip and la.successful = false and la.attemptedAt > :since")
    long countFailedAttemptsByIpSince(String ip, OffsetDateTime since);

    void deleteByAttemptedAtBefore(OffsetDateTime before);
}
