package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, UUID> {
    List<TicketAttachment> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);
}
