package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.EmailConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailConfirmationTokenRepository extends JpaRepository<EmailConfirmationToken, UUID> {
    Optional<EmailConfirmationToken> findByToken(String token);
    void deleteByUserId(UUID userId);
}
