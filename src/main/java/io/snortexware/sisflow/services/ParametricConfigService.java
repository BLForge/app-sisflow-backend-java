package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.*;
import io.snortexware.sisflow.entities.TicketPriorityConfig;
import io.snortexware.sisflow.entities.TicketStatusConfig;
import io.snortexware.sisflow.entities.TicketTypeConfig;
import io.snortexware.sisflow.repositories.TicketPriorityConfigRepository;
import io.snortexware.sisflow.repositories.TicketStatusConfigRepository;
import io.snortexware.sisflow.repositories.TicketTypeConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParametricConfigService {

    private final TicketStatusConfigRepository statusRepository;
    private final TicketPriorityConfigRepository priorityRepository;
    private final TicketTypeConfigRepository typeRepository;

    // --- Status ---

    public List<TicketStatusConfig> listStatuses() {
        return statusRepository.findAll();
    }

    @Transactional
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
    public TicketStatusConfig updateStatus(UUID id, UpdateTicketStatusRequest request) {
        TicketStatusConfig entity = statusRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found"));
        entity.setName(request.getName());
        entity.setColor(request.getColor());
        entity.setDefault(request.isDefault());
        entity.setClosed(request.isClosed());
        entity.setSortOrder(request.getSortOrder());
        return statusRepository.save(entity);
    }

    @Transactional
    public void deleteStatus(UUID id) {
        if (!statusRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found");
        }
        if (statusRepository.existsTicketReferencingStatus(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Status is referenced by existing tickets");
        }
        statusRepository.deleteById(id);
    }

    // --- Priority ---

    public List<TicketPriorityConfig> listPriorities() {
        return priorityRepository.findAll();
    }

    @Transactional
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
    public TicketPriorityConfig updatePriority(UUID id, UpdateTicketPriorityRequest request) {
        TicketPriorityConfig entity = priorityRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Priority not found"));
        entity.setName(request.getName());
        entity.setColor(request.getColor());
        entity.setSlaMultiplier(request.getSlaMultiplier());
        entity.setSortOrder(request.getSortOrder());
        return priorityRepository.save(entity);
    }

    @Transactional
    public void deletePriority(UUID id) {
        if (!priorityRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Priority not found");
        }
        if (priorityRepository.existsTicketReferencingPriority(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Priority is referenced by existing tickets");
        }
        priorityRepository.deleteById(id);
    }

    // --- Type ---

    public List<TicketTypeConfig> listTypes() {
        return typeRepository.findAll();
    }

    @Transactional
    public TicketTypeConfig createType(CreateTicketTypeRequest request) {
        TicketTypeConfig entity = TicketTypeConfig.builder()
                .name(request.getName())
                .icon(request.getIcon())
                .description(request.getDescription())
                .build();
        return typeRepository.save(entity);
    }

    @Transactional
    public TicketTypeConfig updateType(UUID id, UpdateTicketTypeRequest request) {
        TicketTypeConfig entity = typeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Type not found"));
        entity.setName(request.getName());
        entity.setIcon(request.getIcon());
        entity.setDescription(request.getDescription());
        return typeRepository.save(entity);
    }

    @Transactional
    public void deleteType(UUID id) {
        if (!typeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Type not found");
        }
        if (typeRepository.existsTicketReferencingType(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Type is referenced by existing tickets");
        }
        typeRepository.deleteById(id);
    }
}
