package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);

    List<Role> findByIsActiveTrue();

    List<Role> findByIsSystemTrue();

    @Query("SELECT r FROM Role r WHERE r.hierarchyLevel >= :level AND r.isActive = true ORDER BY r.hierarchyLevel ASC")
    List<Role> findByHierarchyLevelGreaterThanOrEqual(@Param("level") Integer level);

    @Query("SELECT r FROM Role r WHERE r.hierarchyLevel < :level AND r.isActive = true ORDER BY r.hierarchyLevel DESC")
    List<Role> findByHierarchyLevelLessThan(@Param("level") Integer level);

    @Query("SELECT r FROM Role r WHERE r.code IN :codes AND r.isActive = true")
    List<Role> findByCodes(@Param("codes") List<String> codes);
}
