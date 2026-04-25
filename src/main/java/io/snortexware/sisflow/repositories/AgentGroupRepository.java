package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.AgentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgentGroupRepository extends JpaRepository<AgentGroup, UUID> {
}
