package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateGithubConfigurationRequest;
import io.snortexware.sisflow.dto.UpdateGithubConfigurationRequest;
import io.snortexware.sisflow.entities.GithubConfiguration;
import io.snortexware.sisflow.entities.Project;
import io.snortexware.sisflow.repositories.GithubConfigurationRepository;
import io.snortexware.sisflow.repositories.ProjectRepository;
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
@RequestMapping("/github-configurations")
public class GithubConfigurationController {

    private final GithubConfigurationRepository githubConfigurationRepository;
    private final ProjectRepository projectRepository;
    private final AuthorizationService authorizationService;

    public GithubConfigurationController(GithubConfigurationRepository githubConfigurationRepository,
                                        ProjectRepository projectRepository,
                                        AuthorizationService authorizationService) {
        this.githubConfigurationRepository = githubConfigurationRepository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<GithubConfiguration> create(
            @Valid @RequestBody CreateGithubConfigurationRequest request,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can create GitHub configurations
        try {
            authorizationService.validateCanManageProjects(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Project> project = projectRepository.findById(request.getProjectId());
        if (project.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // Check if configuration already exists for this project
        if (githubConfigurationRepository.findByProjectId(request.getProjectId()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        GithubConfiguration config = GithubConfiguration.builder()
                .project(project.get())
                .webhookSecret(request.getWebhookSecret())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(githubConfigurationRepository.save(config));
    }

    @GetMapping
    public ResponseEntity<List<GithubConfiguration>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view GitHub configurations
        try {
            authorizationService.validateCanManageProjects(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(githubConfigurationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GithubConfiguration> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view GitHub configurations
        try {
            authorizationService.validateCanManageProjects(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return githubConfigurationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<GithubConfiguration> getByProject(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view GitHub configurations
        try {
            authorizationService.validateCanManageProjects(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return githubConfigurationRepository.findByProjectId(projectId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<GithubConfiguration> update(
            @PathVariable UUID id, 
            @Valid @RequestBody UpdateGithubConfigurationRequest request,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can update GitHub configurations
        try {
            authorizationService.validateCanManageProjects(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<GithubConfiguration> existingConfig = githubConfigurationRepository.findById(id);
        if (existingConfig.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        GithubConfiguration config = existingConfig.get();
        if (request.getWebhookSecret() != null) {
            config.setWebhookSecret(request.getWebhookSecret());
        }
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }

        return ResponseEntity.ok(githubConfigurationRepository.save(config));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can delete GitHub configurations
        try {
            authorizationService.validateCanManageProjects(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!githubConfigurationRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        githubConfigurationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
