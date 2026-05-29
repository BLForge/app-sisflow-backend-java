package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    @EntityGraph(attributePaths = {"role", "role.permissions"})
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.isActive = true")
    List<UserRole> findActiveByUserId(@Param("userId") UUID userId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.id = :roleId")
    Optional<UserRole> findByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.id = :roleId")
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<UserRole> findByUserIdAndRoleIdWithLock(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    void deleteByUserId(UUID userId);
    
    @Query("SELECT ur FROM UserRole ur WHERE ur.role.id = :roleId AND ur.isActive = true")
    List<UserRole> findActiveByRoleId(@Param("roleId") UUID roleId);

    @Query("""
    	    SELECT ur FROM UserRole ur
    	    WHERE ur.user.id = :userId
    	      AND ur.role.id = :roleId
    	      AND ur.isActive = true
    	""")
    Optional<UserRole> findActiveByUserIdAndRoleId(UUID userId, UUID roleId);
    
    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.isActive = true")
    List<UserRole> findByUserId(@Param("userId") UUID userId);

    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.code = :roleCode AND ur.isActive = true")
    boolean userHasRole(@Param("userId") UUID userId, @Param("roleCode") String roleCode);

    @Query("SELECT ur FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.hierarchyLevel >= :level AND ur.isActive = true")
    List<UserRole> findByUserIdAndHierarchyLevelGreaterThanOrEqual(@Param("userId") UUID userId, @Param("level") Integer level);


}
