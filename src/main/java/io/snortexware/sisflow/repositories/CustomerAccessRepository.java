package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.CustomerAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomerAccessRepository extends JpaRepository<CustomerAccess, UUID> {
    List<CustomerAccess> findByCustomerIdOrderByCreatedAtAsc(UUID customerId);
}
