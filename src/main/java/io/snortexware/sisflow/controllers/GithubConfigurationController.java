package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateGithubConfigurationRequest;
import io.snortexware.sisflow.dto.UpdateGithubConfigurationRequest;
import io.snortexware.sisflow.entities.GithubConfiguration;
import io.snortexware.sisflow.entities.Project;
import io.snortexware.sisflow.repositories.GithubConfigurationRepository;
import io.snortexware.sisflow.repositories.ProjectRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/github-configurations")
@RequiredArgsConstructor
public class GithubConfigurationController {

    private final GithubConfigurationRepository githubConfigurationRepository;
    private final ProjectRepository projectRepository;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Transactional
    public ResponseEntity<GithubConfiguration> create(@Valid @RequestBody CreateGithubConfigurationRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);

        Project project = projectRepository.findById(request.getProjectId()).orElseThrow(AppException::badRequest);
        if (githubConfigurationRepository.findByProjectId(request.getProjectId()).isPresent())
            throw AppException.conflict();

        GithubConfiguration config = GithubConfiguration.builder()
                .project(project).webhookSecret(request.getWebhookSecret())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(githubConfigurationRepository.save(config));
    }

    @GetMapping
    public ResponseEntity<List<GithubConfiguration>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        return ResponseEntity.ok(githubConfigurationRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GithubConfiguration> getById(@PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        return ResponseEntity.ok(githubConfigurationRepository.findById(id).orElseThrow(AppException::notFound));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<GithubConfiguration> getByProject(@PathVariable UUID projectId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        return ResponseEntity.ok(githubConfigurationRepository.findByProjectId(projectId)
                .orElseThrow(AppException::notFound));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<GithubConfiguration> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateGithubConfigurationRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);

        GithubConfiguration config = githubConfigurationRepository.findById(id).orElseThrow(AppException::notFound);
        if (request.getWebhookSecret() != null) config.setWebhookSecret(request.getWebhookSecret());
        if (request.getEnabled() != null) config.setEnabled(request.getEnabled());

        return ResponseEntity.ok(githubConfigurationRepository.save(config));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        if (!githubConfigurationRepository.existsById(id)) throw AppException.notFound();
        githubConfigurationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
