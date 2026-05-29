package io.snortexware.sisflow.auth.infrastructure.persistence;

import io.snortexware.sisflow.auth.domain.model.EmailConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataEmailConfirmationTokenRepository extends JpaRepository<EmailConfirmationToken, UUID> {
    Optional<EmailConfirmationToken> findByToken(String token);
    void deleteByUserId(UUID userId);
}
