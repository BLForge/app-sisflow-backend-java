package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateGithubConfigurationRequest;
import io.snortexware.sisflow.dto.UpdateGithubConfigurationRequest;
import io.snortexware.sisflow.entities.GithubConfiguration;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.GithubConfigurationService;
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

    private final GithubConfigurationService githubConfigurationService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Transactional
    public ResponseEntity<GithubConfiguration> create(@Valid @RequestBody CreateGithubConfigurationRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(githubConfigurationService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<GithubConfiguration>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        return ResponseEntity.ok(githubConfigurationService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GithubConfiguration> getById(@PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        return ResponseEntity.ok(githubConfigurationService.getById(id));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<GithubConfiguration> getByProject(@PathVariable UUID projectId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        return ResponseEntity.ok(githubConfigurationService.getByProject(projectId));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<GithubConfiguration> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateGithubConfigurationRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        return ResponseEntity.ok(githubConfigurationService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageProjects(callerId);
        githubConfigurationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
