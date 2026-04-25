package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateAgentGroupRequest;
import io.snortexware.sisflow.dto.UpdateAgentGroupRequest;
import io.snortexware.sisflow.entities.AgentGroup;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.AgentGroupRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentGroupService {

    private final AgentGroupRepository agentGroupRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public AgentGroup create(CreateAgentGroupRequest request) {
        AgentGroup group = AgentGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdAt(OffsetDateTime.now())
                .build();
        return agentGroupRepository.save(group);
    }

    @Transactional
    public AgentGroup update(UUID id, UpdateAgentGroupRequest request) {
        AgentGroup group = agentGroupRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent group not found"));
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        return agentGroupRepository.save(group);
    }

    @Transactional
    public void delete(UUID id) {
        if (!agentGroupRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent group not found");
        }
        agentGroupRepository.deleteById(id);
    }

    @Transactional
    public AgentGroup addMember(UUID groupId, UUID userId) {
        AgentGroup group = agentGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent group not found"));
        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        group.getMembers().add(user);
        return agentGroupRepository.save(group);
    }

    @Transactional
    public AgentGroup removeMember(UUID groupId, UUID userId) {
        AgentGroup group = agentGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent group not found"));
        group.getMembers().removeIf(m -> m.getId().equals(userId));
        return agentGroupRepository.save(group);
    }

    public List<AgentGroup> list() {
        return agentGroupRepository.findAll();
    }
}
