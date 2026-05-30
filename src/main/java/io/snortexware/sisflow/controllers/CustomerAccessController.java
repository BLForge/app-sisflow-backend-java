package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateCustomerAccessRequest;
import io.snortexware.sisflow.entities.CustomerAccess;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.CustomerAccessService;
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
@RequestMapping("/customers/{customerId}/accesses")
@RequiredArgsConstructor
public class CustomerAccessController {

    private final CustomerAccessService customerAccessService;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<CustomerAccess>> list(@PathVariable UUID customerId, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(customerAccessService.list(customerId));
    }

    @PostMapping
    @Transactional
    @CacheEvict(value = "customerAccesses", allEntries = true)
    public ResponseEntity<CustomerAccess> create(@PathVariable UUID customerId,
            @Valid @RequestBody CreateCustomerAccessRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.status(HttpStatus.CREATED).body(customerAccessService.create(customerId, request));
    }

    @DeleteMapping("/{id}")
    @Transactional
    @CacheEvict(value = "customerAccesses", allEntries = true)
    public ResponseEntity<Void> delete(@PathVariable UUID customerId, @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        customerAccessService.delete(customerId, id);
        return ResponseEntity.noContent().build();
    }
}
