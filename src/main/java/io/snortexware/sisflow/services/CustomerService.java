package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateCustomerRequest;
import io.snortexware.sisflow.dto.UpdateCustomerRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final TenantContext tenantContext;

    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public Customer create(CreateCustomerRequest request) {
        if (customerRepository.findByDocument(request.getDocument()).isPresent()) {
            throw AppException.conflict();
        }

        UUID tenantId = tenantContext.getCurrentTenant();
        Tenant tenant = tenantId != null
                ? tenantRepository.findById(tenantId).orElseThrow(AppException::notFound)
                : null;

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
                .tenant(tenant)
                .status(Customer.Status.active)
                .createdAt(OffsetDateTime.now())
                .build();

        return customerRepository.save(customer);
    }

    @Cacheable(value = "customers", key = "@cacheKeyService.tenantKey('all')")
    public List<Customer> list() {
        UUID tenantId = tenantContext.getCurrentTenant();
        return tenantId != null
                ? customerRepository.findByTenant_Id(tenantId)
                : customerRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "customers", allEntries = true)
    public Customer update(UUID id, UpdateCustomerRequest request) {
        Customer customer = customerRepository.findById(id).orElseThrow(AppException::notFound);

        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant != null && customer.getTenant() != null
                && !customer.getTenant().getId().equals(callerTenant)) {
            throw AppException.forbidden();
        }

        if (!customer.getDocument().equals(request.getDocument())
                && customerRepository.findByDocument(request.getDocument()).isPresent()) {
            throw AppException.conflict();
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

        return customerRepository.save(customer);
    }
}
