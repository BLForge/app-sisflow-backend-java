package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.GithubConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GithubConfigurationRepository extends JpaRepository<GithubConfiguration, UUID> {
    Optional<GithubConfiguration> findByProjectId(UUID projectId);
}
