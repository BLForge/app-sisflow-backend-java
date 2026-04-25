package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.System;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SystemRepository extends JpaRepository<System, UUID> {
    List<System> findByCustomerId(UUID customerId);
}
