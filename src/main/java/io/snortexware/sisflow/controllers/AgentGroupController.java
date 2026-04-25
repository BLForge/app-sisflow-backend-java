package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateAgentGroupRequest;
import io.snortexware.sisflow.dto.UpdateAgentGroupRequest;
import io.snortexware.sisflow.entities.AgentGroup;
import io.snortexware.sisflow.services.AgentGroupService;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/agent-groups")
public class AgentGroupController {

    private final AgentGroupService agentGroupService;
    private final AuthorizationService authorizationService;

    public AgentGroupController(AgentGroupService agentGroupService, AuthorizationService authorizationService) {
        this.agentGroupService = agentGroupService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<AgentGroup>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can view agent groups
            if (authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.ok(agentGroupService.list());
            } else {
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping
    public ResponseEntity<AgentGroup> create(@Valid @RequestBody CreateAgentGroupRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can create agent groups
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(agentGroupService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgentGroup> update(@PathVariable UUID id,
                                              @Valid @RequestBody UpdateAgentGroupRequest request,
                                              @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can update agent groups
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(agentGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can delete agent groups
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        agentGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<AgentGroup> addMember(@PathVariable UUID id,
                                                 @RequestBody Map<String, UUID> body,
                                                 @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can manage group members
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        UUID userId = body.get("userId");
        return ResponseEntity.ok(agentGroupService.addMember(id, userId));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id,
                                              @PathVariable UUID userId,
                                              @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can manage group members
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        agentGroupService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }
}
