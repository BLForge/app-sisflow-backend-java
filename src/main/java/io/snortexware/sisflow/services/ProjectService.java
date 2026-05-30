package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateProjectRequest;
import io.snortexware.sisflow.dto.UpdateProjectRequest;
import io.snortexware.sisflow.entities.Project;
import io.snortexware.sisflow.entities.System;
import io.snortexware.sisflow.entities.TicketStatusConfig;
import io.snortexware.sisflow.repositories.ProjectRepository;
import io.snortexware.sisflow.repositories.SystemRepository;
import io.snortexware.sisflow.repositories.TicketStatusConfigRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SystemRepository systemRepository;
    private final TicketStatusConfigRepository ticketStatusConfigRepository;

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public Project create(CreateProjectRequest request) {
        System system = systemRepository.findById(request.getSystemId()).orElseThrow(AppException::badRequest);

        TicketStatusConfig pullRequestStatus = null;
        if (request.getPullRequestStatusId() != null) {
            pullRequestStatus = ticketStatusConfigRepository.findById(request.getPullRequestStatusId())
                    .orElseThrow(AppException::badRequest);
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .system(system)
                .githubRepository(request.getGithubRepository())
                .githubOwner(request.getGithubOwner())
                .pullRequestStatus(pullRequestStatus)
                .status(Project.Status.active)
                .build();

        return projectRepository.save(project);
    }

    @Cacheable(value = "projects", key = "@cacheKeyService.tenantKey('all')")
    public List<Project> list() {
        return projectRepository.findAll();
    }

    @Cacheable(value = "projects", key = "@cacheKeyService.tenantKey('id', #id)")
    public Project getById(UUID id) {
        return projectRepository.findById(id).orElseThrow(AppException::notFound);
    }

    @Cacheable(value = "projects", key = "@cacheKeyService.tenantKey('system', #systemId)")
    public List<Project> listBySystem(UUID systemId) {
        return projectRepository.findBySystemId(systemId);
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public Project update(UUID id, UpdateProjectRequest request) {
        Project project = projectRepository.findById(id).orElseThrow(AppException::notFound);

        if (request.getPullRequestStatusId() != null) {
            project.setPullRequestStatus(ticketStatusConfigRepository.findById(request.getPullRequestStatusId())
                    .orElseThrow(AppException::badRequest));
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setGithubRepository(request.getGithubRepository());
        project.setGithubOwner(request.getGithubOwner());
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        return projectRepository.save(project);
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public void delete(UUID id) {
        if (!projectRepository.existsById(id)) {
            throw AppException.notFound();
        }
        projectRepository.deleteById(id);
    }
}
