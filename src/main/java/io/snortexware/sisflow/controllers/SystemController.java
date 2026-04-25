package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateSystemRequest;
import io.snortexware.sisflow.dto.UpdateSystemRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.System;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.SystemRepository;
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
@RequestMapping("/systems")
public class SystemController {

    private final SystemRepository systemRepository;
    private final CustomerRepository customerRepository;
    private final AuthorizationService authorizationService;

    public SystemController(SystemRepository systemRepository, CustomerRepository customerRepository, AuthorizationService authorizationService) {
        this.systemRepository = systemRepository;
        this.customerRepository = customerRepository;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<System> create(@Valid @RequestBody CreateSystemRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can create systems
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<Customer> customer = customerRepository.findById(request.getCustomerId());
        if (customer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        System system = System.builder()
                .name(request.getName())
                .description(request.getDescription())
                .customer(customer.get())
                .version(request.getVersion())
                .url(request.getUrl())
                .status(System.Status.active)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(systemRepository.save(system));
    }

    @GetMapping
    public ResponseEntity<List<System>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can view all systems
            if (authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.ok(systemRepository.findAll());
            } else {
                // Non-admins get empty list
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<System> getById(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can view systems
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return systemRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<System>> listByCustomer(@PathVariable UUID customerId, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can view systems
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(systemRepository.findByCustomerId(customerId));
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<System> update(@PathVariable UUID id, @Valid @RequestBody UpdateSystemRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can update systems
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Optional<System> existingSystem = systemRepository.findById(id);
        if (existingSystem.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        System system = existingSystem.get();
        system.setName(request.getName());
        system.setDescription(request.getDescription());
        system.setVersion(request.getVersion());
        system.setUrl(request.getUrl());
        if (request.getStatus() != null) {
            system.setStatus(request.getStatus());
        }

        return ResponseEntity.ok(systemRepository.save(system));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only admins can delete systems
            if (!authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (!systemRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        systemRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
