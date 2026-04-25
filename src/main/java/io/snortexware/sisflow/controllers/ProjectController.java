package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateProjectRequest;
import io.snortexware.sisflow.dto.UpdateProjectRequest;
import io.snortexware.sisflow.entities.Project;
import io.snortexware.sisflow.entities.System;
import io.snortexware.sisflow.entities.TicketStatusConfig;
import io.snortexware.sisflow.repositories.ProjectRepository;
import io.snortexware.sisflow.repositories.SystemRepository;
import io.snortexware.sisflow.repositories.TicketStatusConfigRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final SystemRepository systemRepository;
    private final TicketStatusConfigRepository ticketStatusConfigRepository;
    private final AuthorizationService authorizationService;

    public ProjectController(ProjectRepository projectRepository, 
                           SystemRepository systemRepository,
                           TicketStatusConfigRepository ticketStatusConfigRepository,
                           AuthorizationService authorizationService) {
        this.projectRepository = projectRepository;
        this.systemRepository = systemRepository;
        this.ticketStatusConfigRepository = ticketStatusConfigRepository;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Project> create(@Valid @RequestBody CreateProjectRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can create projects
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<System> system = systemRepository.findById(request.getSystemId());
        if (system.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        TicketStatusConfig pullRequestStatus = null;
        if (request.getPullRequestStatusId() != null) {
            Optional<TicketStatusConfig> statusConfig = ticketStatusConfigRepository.findById(request.getPullRequestStatusId());
            if (statusConfig.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            pullRequestStatus = statusConfig.get();
        }

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .system(system.get())
                .githubRepository(request.getGithubRepository())
                .githubOwner(request.getGithubOwner())
                .pullRequestStatus(pullRequestStatus)
                .status(Project.Status.active)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(projectRepository.save(project));
    }

    @GetMapping
    public ResponseEntity<List<Project>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can view all projects
            if (authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.ok(projectRepository.findAll());
            } else {
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getById(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can view projects
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return projectRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/system/{systemId}")
    public ResponseEntity<List<Project>> listBySystem(@PathVariable UUID systemId, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can view projects
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(projectRepository.findBySystemId(systemId));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Project> update(@PathVariable UUID id, @Valid @RequestBody UpdateProjectRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can update projects
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Project> existingProject = projectRepository.findById(id);
        if (existingProject.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Project project = existingProject.get();

        if (request.getPullRequestStatusId() != null) {
            Optional<TicketStatusConfig> statusConfig = ticketStatusConfigRepository.findById(request.getPullRequestStatusId());
            if (statusConfig.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            project.setPullRequestStatus(statusConfig.get());
        }

        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setGithubRepository(request.getGithubRepository());
        project.setGithubOwner(request.getGithubOwner());
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        return ResponseEntity.ok(projectRepository.save(project));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can delete projects
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (!projectRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
