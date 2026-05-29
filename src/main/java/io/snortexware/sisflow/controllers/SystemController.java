package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateSystemRequest;
import io.snortexware.sisflow.dto.UpdateSystemRequest;
import io.snortexware.sisflow.entities.System;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.SystemService;
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
@RequestMapping("/systems")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Transactional
    @CacheEvict(value = "systems", allEntries = true)
    public ResponseEntity<System> create(@Valid @RequestBody CreateSystemRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.status(HttpStatus.CREATED).body(systemService.create(request));
    }

    @GetMapping
    @Cacheable(value = "systems", key = "@cacheKeyService.tenantKey('all')")
    public ResponseEntity<List<System>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(systemService.list());
    }

    @GetMapping("/{id}")
    @Cacheable(value = "systems", key = "@cacheKeyService.tenantKey('id', #id)")
    public ResponseEntity<System> getById(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(systemService.getById(id));
    }

    @GetMapping("/customer/{customerId}")
    @Cacheable(value = "systems", key = "@cacheKeyService.tenantKey('customer', #customerId)")
    public ResponseEntity<List<System>> listByCustomer(@PathVariable UUID customerId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(systemService.listByCustomer(customerId));
    }

    @PutMapping("/{id}")
    @Transactional
    @CacheEvict(value = "systems", allEntries = true)
    public ResponseEntity<System> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateSystemRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(systemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @CacheEvict(value = "systems", allEntries = true)
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        systemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
