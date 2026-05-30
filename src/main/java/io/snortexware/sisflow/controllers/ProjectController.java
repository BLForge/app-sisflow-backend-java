package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateProjectRequest;
import io.snortexware.sisflow.dto.UpdateProjectRequest;
import io.snortexware.sisflow.entities.Project;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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

    private final ProjectService projectService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ResponseEntity<Project> create(@Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Project>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(projectService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Project> getById(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(projectService.getById(id));
    }

    @GetMapping("/system/{systemId}")
    public ResponseEntity<List<Project>> listBySystem(@PathVariable UUID systemId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(projectService.listBySystem(systemId));
    }

    @PutMapping("/{id}")
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ResponseEntity<Project> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(projectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
