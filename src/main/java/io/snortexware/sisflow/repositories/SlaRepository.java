package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.Sla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SlaRepository extends JpaRepository<Sla, UUID> {
    List<Sla> findByTenant_Id(UUID tenantId);
}
