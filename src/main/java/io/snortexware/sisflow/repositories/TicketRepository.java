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

    /** All tickets for a tenant (moderators+). */
    @Query("SELECT t FROM Ticket t WHERE t.customer.tenant.id = :tenantId")
    List<Ticket> findByTenantId(@Param("tenantId") UUID tenantId);

    /** Tickets visible to a regular user within a tenant. */
    @Query("SELECT t FROM Ticket t WHERE t.customer.tenant.id = :tenantId AND (t.createdBy.id = :userId OR t.assignedTo.id = :userId)")
    List<Ticket> findByTenantIdAndUserId(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId);

    /** Tickets visible to a regular user (no tenant restriction — system admin). */
    @Query("SELECT t FROM Ticket t WHERE t.createdBy.id = :userId OR t.assignedTo.id = :userId")
    List<Ticket> findByUserId(@Param("userId") UUID userId);

    /** Single ticket by ID scoped to tenant. */
    @Query("SELECT t FROM Ticket t WHERE t.id = :id AND t.customer.tenant.id = :tenantId")
    Optional<Ticket> findByIdAndTenantId(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
