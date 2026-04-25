package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.TicketStatusConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TicketStatusConfigRepository extends JpaRepository<TicketStatusConfig, UUID> {

    @Query("SELECT COUNT(t) > 0 FROM Ticket t WHERE t.status.id = :statusId")
    boolean existsTicketReferencingStatus(@Param("statusId") UUID statusId);
}
