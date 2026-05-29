package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.*;
import io.snortexware.sisflow.entities.TicketPriorityConfig;
import io.snortexware.sisflow.entities.TicketStatusConfig;
import io.snortexware.sisflow.entities.TicketTypeConfig;
import io.snortexware.sisflow.repositories.TicketPriorityConfigRepository;
import io.snortexware.sisflow.repositories.TicketStatusConfigRepository;
import io.snortexware.sisflow.repositories.TicketTypeConfigRepository;
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
public class ParametricConfigService {

    private final TicketStatusConfigRepository statusRepository;
    private final TicketPriorityConfigRepository priorityRepository;
    private final TicketTypeConfigRepository typeRepository;

    @Cacheable(value = "ticketConfigs", key = "@cacheKeyService.tenantKey('statuses')")
    public List<TicketStatusConfig> listStatuses() {
        return statusRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "ticketConfigs", allEntries = true)
    public TicketStatusConfig createStatus(CreateTicketStatusRequest request) {
        TicketStatusConfig entity = TicketStatusConfig.builder()
                .name(request.getName())
                .color(request.getColor())
                .isDefault(request.isDefault())
                .isClosed(request.isClosed())
                .sortOrder(request.getSortOrder())
                .build();
        return statusRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "ticketConfigs", allEntries = true)
    public TicketStatusConfig updateStatus(UUID id, UpdateTicketStatusRequest request) {
        TicketStatusConfig entity = statusRepository.findById(id)
                .orElseThrow(AppException::notFound);
        entity.setName(request.getName());
        entity.setColor(request.getColor());
        entity.setDefault(request.isDefault());
        entity.setClosed(request.isClosed());
        entity.setSortOrder(request.getSortOrder());
        return statusRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "ticketConfigs", allEntries = true)
    public void deleteStatus(UUID id) {
        if (!statusRepository.existsById(id)) throw AppException.notFound();
        if (statusRepository.existsTicketReferencingStatus(id)) throw AppException.conflict();
        statusRepository.deleteById(id);
    }

    @Cacheable(value = "ticketConfigs", key = "@cacheKeyService.tenantKey('priorities')")
    public List<TicketPriorityConfig> listPriorities() {
        return priorityRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "ticketConfigs", allEntries = true)
    public TicketPriorityConfig createPriority(CreateTicketPriorityRequest request) {
        TicketPriorityConfig entity = TicketPriorityConfig.builder()
                .name(request.getName())
                .color(request.getColor())
                .slaMultiplier(request.getSlaMultiplier())
                .sortOrder(request.getSortOrder())
                .build();
        return priorityRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "ticketConfigs", allEntries = true)
    public TicketPriorityConfig updatePriority(UUID id, UpdateTicketPriorityRequest request) {
        TicketPriorityConfig entity = priorityRepository.findById(id)
                .orElseThrow(AppException::notFound);
        entity.setName(request.getName());
        entity.setColor(request.getColor());
        entity.setSlaMultiplier(request.getSlaMultiplier());
        entity.setSortOrder(request.getSortOrder());
        return priorityRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "ticketConfigs", allEntries = true)
    public void deletePriority(UUID id) {
        if (!priorityRepository.existsById(id)) throw AppException.notFound();
        if (priorityRepository.existsTicketReferencingPriority(id)) throw AppException.conflict();
        priorityRepository.deleteById(id);
    }

    @Cacheable(value = "ticketConfigs", key = "@cacheKeyService.tenantKey('types')")
    public List<TicketTypeConfig> listTypes() {
        return typeRepository.findAll();
    }

    @Transactional
    @CacheEvict(value = "ticketConfigs", allEntries = true)
    public TicketTypeConfig createType(CreateTicketTypeRequest request) {
        TicketTypeConfig entity = TicketTypeConfig.builder()
                .name(request.getName())
                .icon(request.getIcon())
                .description(request.getDescription())
                .build();
        return typeRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "ticketConfigs", allEntries = true)
    public TicketTypeConfig updateType(UUID id, UpdateTicketTypeRequest request) {
        TicketTypeConfig entity = typeRepository.findById(id)
                .orElseThrow(AppException::notFound);
        entity.setName(request.getName());
        entity.setIcon(request.getIcon());
        entity.setDescription(request.getDescription());
        return typeRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "ticketConfigs", allEntries = true)
    public void deleteType(UUID id) {
        if (!typeRepository.existsById(id)) throw AppException.notFound();
        if (typeRepository.existsTicketReferencingType(id)) throw AppException.conflict();
        typeRepository.deleteById(id);
    }
}
