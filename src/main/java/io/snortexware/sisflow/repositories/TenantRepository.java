package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findByDomain(String domain);

    @Query("""
            select count(t) > 0
            from Tenant t
            where t.id = :tenantId
              and (t.logoUrl = :fileUrl or t.logoIconUrl = :fileUrl or t.backgroundUrl = :fileUrl)
            """)
    boolean existsTenantBrandingFile(@Param("tenantId") UUID tenantId, @Param("fileUrl") String fileUrl);
}
