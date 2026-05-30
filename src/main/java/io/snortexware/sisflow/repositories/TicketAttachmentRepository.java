package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, UUID> {
    List<TicketAttachment> findByTicketIdOrderByCreatedAtDesc(UUID ticketId);

    @Query("select ta from TicketAttachment ta join fetch ta.ticket where ta.fileUrl = :fileUrl")
    Optional<TicketAttachment> findByFileUrlWithTicket(@Param("fileUrl") String fileUrl);
}
