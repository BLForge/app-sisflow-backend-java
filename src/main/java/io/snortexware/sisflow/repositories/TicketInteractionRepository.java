package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.TicketInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketInteractionRepository extends JpaRepository<TicketInteraction, UUID> {
    List<TicketInteraction> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
    List<TicketInteraction> findByTicketIdAndIsInternal(UUID ticketId, boolean isInternal);
}
