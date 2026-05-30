package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateSlaRequest;
import io.snortexware.sisflow.dto.UpdateSlaRequest;
import io.snortexware.sisflow.entities.Sla;
import io.snortexware.sisflow.repositories.SlaRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SlaService {

    private final SlaRepository slaRepository;

    @Cacheable(value = "slas", key = "@cacheKeyService.tenantKey('all')")
    public List<Sla> list() {
        return slaRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "slas", allEntries = true)
    public Sla create(CreateSlaRequest request) {
        return slaRepository.save(Sla.builder()
                .name(request.getName())
                .responseTimeHours(request.getResponseTimeHours())
                .resolutionTimeHours(request.getResolutionTimeHours())
                .build());
    }

    @Transactional
    @CacheEvict(value = "slas", allEntries = true)
    public Sla update(UUID id, UpdateSlaRequest request) {
        Sla sla = slaRepository.findById(id).orElseThrow(AppException::notFound);
        sla.setName(request.getName());
        sla.setResponseTimeHours(request.getResponseTimeHours());
        sla.setResolutionTimeHours(request.getResolutionTimeHours());
        return slaRepository.save(sla);
    }
}
