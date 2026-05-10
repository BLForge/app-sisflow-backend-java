package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateAgentGroupRequest;
import io.snortexware.sisflow.dto.UpdateAgentGroupRequest;
import io.snortexware.sisflow.entities.AgentGroup;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.AgentGroupRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(AppException::notFound);
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        return agentGroupRepository.save(group);
    }

    @Transactional
    public void delete(UUID id) {
        if (!agentGroupRepository.existsById(id)) throw AppException.notFound();
        agentGroupRepository.deleteById(id);
    }

    @Transactional
    public AgentGroup addMember(UUID groupId, UUID userId) {
        AgentGroup group = agentGroupRepository.findById(groupId).orElseThrow(AppException::notFound);
        UserProfile user = userProfileRepository.findById(userId).orElseThrow(AppException::notFound);
        group.getMembers().add(user);
        return agentGroupRepository.save(group);
    }

    @Transactional
    public AgentGroup removeMember(UUID groupId, UUID userId) {
        AgentGroup group = agentGroupRepository.findById(groupId).orElseThrow(AppException::notFound);
        group.getMembers().removeIf(m -> m.getId().equals(userId));
        return agentGroupRepository.save(group);
    }

    public List<AgentGroup> list() {
        return agentGroupRepository.findAll();
    }
}
