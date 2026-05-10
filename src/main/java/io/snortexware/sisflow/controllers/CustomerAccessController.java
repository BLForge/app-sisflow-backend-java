package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateCustomerAccessRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.CustomerAccess;
import io.snortexware.sisflow.repositories.CustomerAccessRepository;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers/{customerId}/accesses")
@RequiredArgsConstructor
public class CustomerAccessController {

    private final CustomerAccessRepository accessRepository;
    private final CustomerRepository customerRepository;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<CustomerAccess>> list(@PathVariable UUID customerId, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(accessRepository.findByCustomerIdOrderByCreatedAtAsc(customerId));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CustomerAccess> create(@PathVariable UUID customerId,
            @Valid @RequestBody CreateCustomerAccessRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();

        Customer customer = customerRepository.findById(customerId).orElseThrow(AppException::notFound);

        CustomerAccess access = CustomerAccess.builder()
                .customer(customer).label(request.getLabel()).type(request.getType())
                .value(request.getValue()).username(request.getUsername())
                .password(request.getPassword()).notes(request.getNotes())
                .createdAt(OffsetDateTime.now()).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(accessRepository.save(access));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID customerId, @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();

        CustomerAccess access = accessRepository.findById(id).orElseThrow(AppException::notFound);
        if (!access.getCustomer().getId().equals(customerId)) throw AppException.notFound();

        accessRepository.delete(access);
        return ResponseEntity.noContent().build();
    }
}
