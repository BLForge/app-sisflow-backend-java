package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.email = :email AND la.successful = false AND la.attemptedAt > :since")
    long countFailedAttemptsSince(String email, OffsetDateTime since);

    @Query("SELECT COUNT(la) FROM LoginAttempt la WHERE la.ipAddress = :ip AND la.successful = false AND la.attemptedAt > :since")
    long countFailedAttemptsByIpSince(String ip, OffsetDateTime since);

    void deleteByAttemptedAtBefore(OffsetDateTime before);
}
