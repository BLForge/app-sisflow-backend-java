package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateCustomerAccessRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.CustomerAccess;
import io.snortexware.sisflow.repositories.CustomerAccessRepository;
import io.snortexware.sisflow.repositories.CustomerRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers/{customerId}/accesses")
@RequiredArgsConstructor
public class CustomerAccessController {

    private final CustomerAccessRepository accessRepository;
    private final CustomerRepository customerRepository;

    @GetMapping
    public ResponseEntity<List<CustomerAccess>> list(@PathVariable UUID customerId) {
        return ResponseEntity.ok(accessRepository.findByCustomerIdOrderByCreatedAtAsc(customerId));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CustomerAccess> create(
            @PathVariable UUID customerId,
            @Valid @RequestBody CreateCustomerAccessRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        
        CustomerAccess access = CustomerAccess.builder()
                .customer(customer)
                .label(request.getLabel())
                .type(request.getType())
                .value(request.getValue())
                .username(request.getUsername())
                .password(request.getPassword())
                .notes(request.getNotes())
                .createdAt(OffsetDateTime.now())
                .build();

        
        return ResponseEntity.status(HttpStatus.CREATED).body(accessRepository.save(access));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID customerId, @PathVariable UUID id) {
        CustomerAccess access = accessRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Access not found"));
        if (!access.getCustomer().getId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Access not found");
        }
        accessRepository.delete(access);
        return ResponseEntity.noContent().build();
    }
}
