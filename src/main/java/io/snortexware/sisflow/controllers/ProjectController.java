package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateProjectRequest;
import io.snortexware.sisflow.dto.UpdateProjectRequest;
import io.snortexware.sisflow.entities.Project;
import io.snortexware.sisflow.entities.System;
import io.snortexware.sisflow.entities.TicketStatusConfig;
import io.snortexware.sisflow.repositories.ProjectRepository;
import io.snortexware.sisflow.repositories.SystemRepository;
import io.snortexware.sisflow.repositories.TicketStatusConfigRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final SystemRepository systemRepository;
    private final TicketStatusConfigRepository ticketStatusConfigRepository;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ResponseEntity<Project> create(@Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();

        System system = systemRepository.findById(request.getSystemId()).orElseThrow(AppException::badRequest);

        TicketStatusConfig pullRequestStatus = null;
        if (request.getPullRequestStatusId() != null)
            pullRequestStatus = ticketStatusConfigRepository.findById(request.getPullRequestStatusId())
                    .orElseThrow(AppException::badRequest);

        Project project = Project.builder()
                .name(request.getName()).description(request.getDescription())
                .system(system).githubRepository(request.getGithubRepository())
                .githubOwner(request.getGithubOwner()).pullRequestStatus(pullRequestStatus)
                .status(Project.Status.active).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(projectRepository.save(project));
    }

    @GetMapping
    @Cacheable(value = "projects", key = "@cacheKeyService.tenantKey('all')")
    public ResponseEntity<List<Project>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(projectRepository.findAll());
    }

    @GetMapping("/{id}")
    @Cacheable(value = "projects", key = "@cacheKeyService.tenantKey('id', #id)")
    public ResponseEntity<Project> getById(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(projectRepository.findById(id).orElseThrow(AppException::notFound));
    }

    @GetMapping("/system/{systemId}")
    @Cacheable(value = "projects", key = "@cacheKeyService.tenantKey('system', #systemId)")
    public ResponseEntity<List<Project>> listBySystem(@PathVariable UUID systemId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(projectRepository.findBySystemId(systemId));
    }

    @PutMapping("/{id}")
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ResponseEntity<Project> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();

        Project project = projectRepository.findById(id).orElseThrow(AppException::notFound);

        if (request.getPullRequestStatusId() != null)
            project.setPullRequestStatus(ticketStatusConfigRepository
                    .findById(request.getPullRequestStatusId()).orElseThrow(AppException::badRequest));

        project.setName(request.getName()); project.setDescription(request.getDescription());
        project.setGithubRepository(request.getGithubRepository());
        project.setGithubOwner(request.getGithubOwner());
        if (request.getStatus() != null) project.setStatus(request.getStatus());

        return ResponseEntity.ok(projectRepository.save(project));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        if (!projectRepository.existsById(id)) throw AppException.notFound();
        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
