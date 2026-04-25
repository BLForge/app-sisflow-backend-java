package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.TicketTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TicketTypeConfigRepository extends JpaRepository<TicketTypeConfig, UUID> {

    @Query("SELECT COUNT(t) > 0 FROM Ticket t WHERE t.typeConfig.id = :typeId")
    boolean existsTicketReferencingType(@Param("typeId") UUID typeId);
}
