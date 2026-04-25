package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateCustomerRequest;
import io.snortexware.sisflow.dto.UpdateCustomerRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final AuthorizationService authorizationService;

    public CustomerController(CustomerRepository customerRepository, AuthorizationService authorizationService) {
        this.customerRepository = customerRepository;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Customer> create(@Valid @RequestBody CreateCustomerRequest request, @AuthenticationPrincipal UUID callerId) {
        // SECURITY: Only admins can create customers
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            if (!authorizationService.isAdminOrAbove(callerId)) {
                log.warn("Non-admin user {} attempted to create customer", callerId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.error("Authorization check failed for user: {}", callerId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        if (customerRepository.findByDocument(request.getDocument()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Customer customer = Customer.builder()
                .name(request.getName())
                .tradeName(request.getTradeName())
                .document(request.getDocument())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .logoUrl(request.getLogoUrl())
                .notes(request.getNotes())
                .status(Customer.Status.active)
                .createdAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(customerRepository.save(customer));
    }

    @GetMapping
    public ResponseEntity<List<Customer>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Check if user has permission to view customers
        try {
            // Only admins can view all customers
            if (authorizationService.isAdminOrAbove(callerId)) {
                return ResponseEntity.ok(customerRepository.findAll());
            } else {
                // Non-admin users get empty list for now
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            // If authorization service fails, return empty list for security
            return ResponseEntity.ok(List.of());
        }
    }
    
    private boolean canUserAccessCustomer(Customer customer, UUID userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            // Only admins can access customers
            return authorizationService.isAdminOrAbove(userId);
        } catch (Exception e) {
            return false;
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Customer> update(@PathVariable UUID id, @Valid @RequestBody UpdateCustomerRequest request, @AuthenticationPrincipal UUID callerId) {
        // SECURITY: Only admins can update customers
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            if (!authorizationService.isAdminOrAbove(callerId)) {
                log.warn("Non-admin user {} attempted to update customer {}", callerId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            log.error("Authorization check failed for user: {}", callerId, e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        Optional<Customer> existingCustomer = customerRepository.findById(id);
        if (existingCustomer.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Customer customer = existingCustomer.get();

        // Check if document is being changed and if the new document already exists
        if (!customer.getDocument().equals(request.getDocument())) {
            if (customerRepository.findByDocument(request.getDocument()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        customer.setName(request.getName());
        customer.setTradeName(request.getTradeName());
        customer.setDocument(request.getDocument());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setAddress(request.getAddress());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setLogoUrl(request.getLogoUrl());
        customer.setNotes(request.getNotes());

        return ResponseEntity.ok(customerRepository.save(customer));
    }
}
