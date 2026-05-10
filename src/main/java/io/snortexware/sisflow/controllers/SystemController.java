package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateSystemRequest;
import io.snortexware.sisflow.dto.UpdateSystemRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.System;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.SystemRepository;
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
@RequestMapping("/systems")
@RequiredArgsConstructor
public class SystemController {

    private final SystemRepository systemRepository;
    private final CustomerRepository customerRepository;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Transactional
    public ResponseEntity<System> create(@Valid @RequestBody CreateSystemRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();

        Customer customer = customerRepository.findById(request.getCustomerId()).orElseThrow(AppException::badRequest);

        System system = System.builder()
                .name(request.getName()).description(request.getDescription())
                .customer(customer).version(request.getVersion())
                .url(request.getUrl()).status(System.Status.active).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(systemRepository.save(system));
    }

    @GetMapping
    public ResponseEntity<List<System>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(systemRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<System> getById(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(systemRepository.findById(id).orElseThrow(AppException::notFound));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<System>> listByCustomer(@PathVariable UUID customerId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(systemRepository.findByCustomerId(customerId));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<System> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateSystemRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();

        System system = systemRepository.findById(id).orElseThrow(AppException::notFound);
        system.setName(request.getName()); system.setDescription(request.getDescription());
        system.setVersion(request.getVersion()); system.setUrl(request.getUrl());
        if (request.getStatus() != null) system.setStatus(request.getStatus());

        return ResponseEntity.ok(systemRepository.save(system));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        if (!systemRepository.existsById(id)) throw AppException.notFound();
        systemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
