package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketStatusConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    List<Ticket> findByCustomerId(UUID customerId);
    List<Ticket> findByAssignedToId(UUID userId);
    List<Ticket> findByStatus(TicketStatusConfig status);
    Optional<Ticket> findByCode(Long code);
    boolean existsByCode(Long code);
    boolean existsByCodeAndIdNot(Long code, UUID id);
    boolean existsByCodeAndCustomer_Tenant_Id(Long code, UUID tenantId);
    boolean existsByCodeAndIdNotAndCustomer_Tenant_Id(Long code, UUID id, UUID tenantId);

    @Query("SELECT COALESCE(MAX(t.code), 1000) FROM Ticket t")
    Long findMaxCode();

    @Query("SELECT t FROM Ticket t WHERE t.assignedTo.id = :userId ORDER BY CASE t.priority WHEN 'critical' THEN 0 WHEN 'high' THEN 1 WHEN 'medium' THEN 2 WHEN 'low' THEN 3 ELSE 4 END ASC, t.createdAt ASC")
    List<Ticket> findMyQueue(@Param("userId") UUID userId);
}
