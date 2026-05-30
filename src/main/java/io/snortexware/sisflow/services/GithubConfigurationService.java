package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateGithubConfigurationRequest;
import io.snortexware.sisflow.dto.UpdateGithubConfigurationRequest;
import io.snortexware.sisflow.entities.GithubConfiguration;
import io.snortexware.sisflow.entities.Project;
import io.snortexware.sisflow.repositories.GithubConfigurationRepository;
import io.snortexware.sisflow.repositories.ProjectRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GithubConfigurationService {

    private final GithubConfigurationRepository githubConfigurationRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    @CacheEvict(value = "githubConfigurations", allEntries = true)
    public GithubConfiguration create(CreateGithubConfigurationRequest request) {
        Project project = projectRepository.findById(request.getProjectId()).orElseThrow(AppException::badRequest);
        if (githubConfigurationRepository.findByProjectId(request.getProjectId()).isPresent()) {
            throw AppException.conflict();
        }

        GithubConfiguration config = GithubConfiguration.builder()
                .project(project)
                .webhookSecret(request.getWebhookSecret())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        return githubConfigurationRepository.save(config);
    }

    @Cacheable(value = "githubConfigurations", key = "@cacheKeyService.tenantKey('all')")
    public List<GithubConfiguration> list() {
        return githubConfigurationRepository.findAll();
    }

    @Cacheable(value = "githubConfigurations", key = "@cacheKeyService.tenantKey('id', #id)")
    public GithubConfiguration getById(UUID id) {
        return githubConfigurationRepository.findById(id).orElseThrow(AppException::notFound);
    }

    @Cacheable(value = "githubConfigurations", key = "@cacheKeyService.tenantKey('project', #projectId)")
    public GithubConfiguration getByProject(UUID projectId) {
        return githubConfigurationRepository.findByProjectId(projectId).orElseThrow(AppException::notFound);
    }

    @Transactional
    @CacheEvict(value = "githubConfigurations", allEntries = true)
    public GithubConfiguration update(UUID id, UpdateGithubConfigurationRequest request) {
        GithubConfiguration config = githubConfigurationRepository.findById(id).orElseThrow(AppException::notFound);
        if (request.getWebhookSecret() != null) {
            config.setWebhookSecret(request.getWebhookSecret());
        }
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        return githubConfigurationRepository.save(config);
    }

    @Transactional
    @CacheEvict(value = "githubConfigurations", allEntries = true)
    public void delete(UUID id) {
        if (!githubConfigurationRepository.existsById(id)) {
            throw AppException.notFound();
        }
        githubConfigurationRepository.deleteById(id);
    }
}
