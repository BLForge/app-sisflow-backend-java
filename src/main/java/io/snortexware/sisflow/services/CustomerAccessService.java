package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateCustomerAccessRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.CustomerAccess;
import io.snortexware.sisflow.repositories.CustomerAccessRepository;
import io.snortexware.sisflow.repositories.CustomerRepository;
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
public class CustomerAccessService {

    private final CustomerAccessRepository customerAccessRepository;
    private final CustomerRepository customerRepository;

    @Cacheable(value = "customerAccesses", key = "@cacheKeyService.tenantKey(#customerId)")
    public List<CustomerAccess> list(UUID customerId) {
        return customerAccessRepository.findByCustomerIdOrderByCreatedAtAsc(customerId);
    }

    @Transactional
    @CacheEvict(value = "customerAccesses", allEntries = true)
    public CustomerAccess create(UUID customerId, CreateCustomerAccessRequest request) {
        Customer customer = customerRepository.findById(customerId).orElseThrow(AppException::notFound);

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

        return customerAccessRepository.save(access);
    }

    @Transactional
    @CacheEvict(value = "customerAccesses", allEntries = true)
    public void delete(UUID customerId, UUID id) {
        CustomerAccess access = customerAccessRepository.findById(id).orElseThrow(AppException::notFound);
        if (!access.getCustomer().getId().equals(customerId)) {
            throw AppException.notFound();
        }
        customerAccessRepository.delete(access);
    }
}
