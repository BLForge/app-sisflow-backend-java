package io.snortexware.sisflow.repositories;
import io.snortexware.sisflow.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    UserProfile getUserProfileById(UUID id);
    Optional<UserProfile> findByEmail(String email);
    List<UserProfile> findByTenant_Id(UUID tenantId);
    List<UserProfile> findByCustomerId(UUID customerId);
    List<UserProfile> findByCustomer_IdNot(UUID customerId);
}