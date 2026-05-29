package io.snortexware.sisflow.auth.infrastructure.persistence;

import io.snortexware.sisflow.auth.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface SpringDataUserRoleRepository extends JpaRepository<UserRole, UUID> {

    @Query("select count(ur) > 0 from UserRole ur join ur.role r where ur.user.id = :userId and r.code = 'system_admin' and ur.isActive = true")
    boolean hasSystemAdminRole(@Param("userId") UUID userId);
}
