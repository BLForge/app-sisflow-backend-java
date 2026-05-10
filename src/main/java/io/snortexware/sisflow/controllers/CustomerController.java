package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateCustomerRequest;
import io.snortexware.sisflow.dto.UpdateCustomerRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;
    private final TenantRepository tenantRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<Customer> create(@Valid @RequestBody CreateCustomerRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        if (customerRepository.findByDocument(request.getDocument()).isPresent()) throw AppException.conflict();

        UUID tenantId = tenantContext.getCurrentTenant();
        Tenant tenant = tenantId != null
                ? tenantRepository.findById(tenantId).orElseThrow(AppException::notFound)
                : null;

        Customer customer = Customer.builder()
                .name(request.getName()).tradeName(request.getTradeName())
                .document(request.getDocument()).email(request.getEmail())
                .phone(request.getPhone()).address(request.getAddress())
                .city(request.getCity()).state(request.getState())
                .logoUrl(request.getLogoUrl()).notes(request.getNotes())
                .tenant(tenant).status(Customer.Status.active)
                .createdAt(OffsetDateTime.now()).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(customerRepository.save(customer));
    }

    @GetMapping
    public ResponseEntity<List<Customer>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();

        UUID tenantId = tenantContext.getCurrentTenant();
        List<Customer> customers = tenantId != null
                ? customerRepository.findByTenant_Id(tenantId)
                : customerRepository.findAll();
        return ResponseEntity.ok(customers);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Customer> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateCustomerRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();

        Customer customer = customerRepository.findById(id).orElseThrow(AppException::notFound);

        if (!customer.getDocument().equals(request.getDocument())
                && customerRepository.findByDocument(request.getDocument()).isPresent())
            throw AppException.conflict();

        customer.setName(request.getName()); customer.setTradeName(request.getTradeName());
        customer.setDocument(request.getDocument()); customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone()); customer.setAddress(request.getAddress());
        customer.setCity(request.getCity()); customer.setState(request.getState());
        customer.setLogoUrl(request.getLogoUrl()); customer.setNotes(request.getNotes());

        return ResponseEntity.ok(customerRepository.save(customer));
    }
}
