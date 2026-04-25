package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.TicketPriorityConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TicketPriorityConfigRepository extends JpaRepository<TicketPriorityConfig, UUID> {

    @Query("SELECT COUNT(t) > 0 FROM Ticket t WHERE t.priorityConfig.id = :priorityId")
    boolean existsTicketReferencingPriority(@Param("priorityId") UUID priorityId);
}
