package io.snortexware.sisflow.auth.infrastructure.persistence;

import io.snortexware.sisflow.auth.domain.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataPasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUserId(UUID userId);
}
