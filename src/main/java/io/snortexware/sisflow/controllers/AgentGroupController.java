package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateAgentGroupRequest;
import io.snortexware.sisflow.dto.UpdateAgentGroupRequest;
import io.snortexware.sisflow.entities.AgentGroup;
import io.snortexware.sisflow.services.AgentGroupService;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/agent-groups")
@RequiredArgsConstructor
public class AgentGroupController extends BaseController {

    private final AgentGroupService agentGroupService;
    private final AuthorizationService authorizationService;

    @Override
    protected AuthorizationService authorizationService() { return authorizationService; }

    @GetMapping
    public ResponseEntity<List<AgentGroup>> list(@AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(agentGroupService.list());
    }

    @PostMapping
    public ResponseEntity<AgentGroup> create(@Valid @RequestBody CreateAgentGroupRequest request,
                                             @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(agentGroupService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgentGroup> update(@PathVariable UUID id,
                                             @Valid @RequestBody UpdateAgentGroupRequest request,
                                             @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(agentGroupService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        agentGroupService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<AgentGroup> addMember(@PathVariable UUID id,
                                                @Valid @RequestBody AddMemberRequest body,
                                                @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(agentGroupService.addMember(id, body.userId()));
    }

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id,
                                             @PathVariable UUID userId,
                                             @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        agentGroupService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    public record AddMemberRequest(@NotNull UUID userId) {}
}
