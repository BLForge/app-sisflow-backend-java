package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByDocument(String document);
    List<Customer> findByTenant_Id(UUID tenantId);
}
