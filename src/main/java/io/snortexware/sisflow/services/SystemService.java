package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateSystemRequest;
import io.snortexware.sisflow.dto.UpdateSystemRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.System;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.SystemRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemService {

    private final SystemRepository systemRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public System create(CreateSystemRequest request) {
        Customer customer = customerRepository.findById(request.getCustomerId()).orElseThrow(AppException::badRequest);

        System system = System.builder()
                .name(request.getName())
                .description(request.getDescription())
                .customer(customer)
                .version(request.getVersion())
                .url(request.getUrl())
                .status(System.Status.active)
                .build();

        return systemRepository.save(system);
    }

    public List<System> list() {
        return systemRepository.findAll();
    }

    public System getById(UUID id) {
        return systemRepository.findById(id).orElseThrow(AppException::notFound);
    }

    public List<System> listByCustomer(UUID customerId) {
        return systemRepository.findByCustomerId(customerId);
    }

    @Transactional
    public System update(UUID id, UpdateSystemRequest request) {
        System system = systemRepository.findById(id).orElseThrow(AppException::notFound);
        system.setName(request.getName());
        system.setDescription(request.getDescription());
        system.setVersion(request.getVersion());
        system.setUrl(request.getUrl());
        if (request.getStatus() != null) {
            system.setStatus(request.getStatus());
        }
        return systemRepository.save(system);
    }

    @Transactional
    public void delete(UUID id) {
        if (!systemRepository.existsById(id)) {
            throw AppException.notFound();
        }
        systemRepository.deleteById(id);
    }
}
