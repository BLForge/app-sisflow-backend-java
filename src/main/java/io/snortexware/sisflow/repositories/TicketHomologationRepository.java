package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.TicketHomologation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketHomologationRepository extends JpaRepository<TicketHomologation, UUID> {
    List<TicketHomologation> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
    Optional<TicketHomologation> findByTicketIdAndUserId(UUID ticketId, UUID userId);
    boolean existsByTicketIdAndUserId(UUID ticketId, UUID userId);
}
