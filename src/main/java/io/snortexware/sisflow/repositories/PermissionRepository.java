package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(String code);

    List<Permission> findByIsActiveTrue();

    List<Permission> findByIsSystemTrue();

    @Query("SELECT p FROM Permission p WHERE p.code IN :codes AND p.isActive = true")
    List<Permission> findByCodes(@Param("codes") List<String> codes);

    @Query("SELECT p FROM Permission p WHERE p.category = :category AND p.isActive = true")
    List<Permission> findByCategory(@Param("category") String category);

    @Query("SELECT p FROM Permission p WHERE p.code LIKE :pattern% AND p.isActive = true")
    List<Permission> findByCodePattern(@Param("pattern") String pattern);
}
