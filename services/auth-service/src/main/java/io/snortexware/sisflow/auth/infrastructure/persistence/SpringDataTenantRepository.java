package io.snortexware.sisflow.auth.infrastructure.persistence;

import io.snortexware.sisflow.auth.domain.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataTenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByDomain(String domain);
}
