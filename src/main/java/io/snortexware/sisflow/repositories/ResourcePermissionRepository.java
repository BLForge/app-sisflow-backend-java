package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.ResourcePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourcePermissionRepository extends JpaRepository<ResourcePermission, UUID> {

    @Query("SELECT rp FROM ResourcePermission rp WHERE rp.resourceId = :resourceId AND rp.user.id = :userId AND rp.action = :action AND rp.isActive = true AND (rp.expiresAt IS NULL OR rp.expiresAt > :now)")
    Optional<ResourcePermission> findActiveByResourceIdAndUserIdAndAction(
        @Param("resourceId") UUID resourceId,
        @Param("userId") UUID userId,
        @Param("action") String action,
        @Param("now") OffsetDateTime now
    );

    @Query("SELECT rp FROM ResourcePermission rp WHERE rp.resourceId = :resourceId AND rp.user.id = :userId AND rp.isActive = true AND (rp.expiresAt IS NULL OR rp.expiresAt > :now)")
    List<ResourcePermission> findActiveByResourceIdAndUserId(
        @Param("resourceId") UUID resourceId,
        @Param("userId") UUID userId,
        @Param("now") OffsetDateTime now
    );

    @Query("SELECT rp FROM ResourcePermission rp WHERE rp.user.id = :userId AND rp.resourceType = :resourceType AND rp.isActive = true AND (rp.expiresAt IS NULL OR rp.expiresAt > :now)")
    List<ResourcePermission> findActiveByUserIdAndResourceType(
        @Param("userId") UUID userId,
        @Param("resourceType") String resourceType,
        @Param("now") OffsetDateTime now
    );

    @Query("SELECT rp FROM ResourcePermission rp WHERE rp.resourceId = :resourceId AND rp.resourceType = :resourceType AND rp.isActive = true AND (rp.expiresAt IS NULL OR rp.expiresAt > :now)")
    List<ResourcePermission> findActiveByResourceIdAndResourceType(
        @Param("resourceId") UUID resourceId,
        @Param("resourceType") String resourceType,
        @Param("now") OffsetDateTime now
    );

    @Query("SELECT rp FROM ResourcePermission rp WHERE rp.expiresAt < :now AND rp.isActive = true")
    List<ResourcePermission> findExpiredPermissions(@Param("now") OffsetDateTime now);

    @Query("SELECT rp FROM ResourcePermission rp WHERE rp.user.id = :userId AND rp.isActive = true AND (rp.expiresAt IS NULL OR rp.expiresAt > :now)")
    List<ResourcePermission> findByUserId(
        @Param("userId") UUID userId,
        @Param("now") OffsetDateTime now
    );

    @Query("SELECT rp FROM ResourcePermission rp WHERE rp.user.id = :userId AND rp.isActive = true")
    List<ResourcePermission> findByUserId(@Param("userId") UUID userId);
}
