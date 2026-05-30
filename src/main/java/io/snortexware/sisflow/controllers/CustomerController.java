package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateCustomerRequest;
import io.snortexware.sisflow.dto.UpdateCustomerRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public ResponseEntity<Customer> create(@Valid @RequestBody CreateCustomerRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Customer>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(customerService.list());
    }

    @PutMapping("/{id}")
    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public ResponseEntity<Customer> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(customerService.update(id, request));
    }
}
