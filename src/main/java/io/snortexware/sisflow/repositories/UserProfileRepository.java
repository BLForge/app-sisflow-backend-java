package io.snortexware.sisflow.repositories;
import io.snortexware.sisflow.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    UserProfile getUserProfileById(UUID id);
    
    List<UserProfile> findByCustomerId(UUID customerId);
    List<UserProfile> findByCustomer_IdNot(UUID customerId);
}