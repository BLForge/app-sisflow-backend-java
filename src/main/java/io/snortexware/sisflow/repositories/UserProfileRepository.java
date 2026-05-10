package io.snortexware.sisflow.repositories;
import io.snortexware.sisflow.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    @Query("SELECT u FROM UserProfile u LEFT JOIN FETCH u.tenant WHERE u.id = :id")
    Optional<UserProfile> findByIdWithTenant(@Param("id") UUID id);

    @Query("SELECT u FROM UserProfile u LEFT JOIN FETCH u.tenant WHERE u.email = :email")
    Optional<UserProfile> findByEmailWithTenant(@Param("email") String email);

    UserProfile getUserProfileById(UUID id);
    Optional<UserProfile> findByEmail(String email);
    List<UserProfile> findByTenant_Id(UUID tenantId);
    List<UserProfile> findByCustomerId(UUID customerId);
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur JOIN ur.role r WHERE ur.user.id = :userId AND r.code = 'system_admin' AND ur.isActive = true")
    boolean hasSystemAdminRole(@Param("userId") UUID userId);
}