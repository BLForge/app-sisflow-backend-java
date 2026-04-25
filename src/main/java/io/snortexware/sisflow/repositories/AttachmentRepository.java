package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
    List<Attachment> findByTicketId(UUID ticketId);
    List<Attachment> findByInteractionId(UUID interactionId);
}
