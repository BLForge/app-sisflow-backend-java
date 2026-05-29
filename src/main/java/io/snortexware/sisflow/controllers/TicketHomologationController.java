package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.AssignHomologatorRequest;
import io.snortexware.sisflow.dto.HomologationDecisionRequest;
import io.snortexware.sisflow.entities.TicketHomologation;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.TicketHomologationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{ticketId}/homologations")
@RequiredArgsConstructor
public class TicketHomologationController {

    private final TicketHomologationService ticketHomologationService;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<TicketHomologation>> list(@PathVariable UUID ticketId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(ticketHomologationService.list(ticketId, callerId));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TicketHomologation> assign(@PathVariable UUID ticketId,
            @Valid @RequestBody AssignHomologatorRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketHomologationService.assign(ticketId, callerId, request));
    }

    @PutMapping("/{id}/decision")
    @Transactional
    public ResponseEntity<TicketHomologation> decide(@PathVariable UUID ticketId, @PathVariable UUID id,
            @Valid @RequestBody HomologationDecisionRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(ticketHomologationService.decide(ticketId, id, callerId, request));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> remove(@PathVariable UUID ticketId, @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        ticketHomologationService.remove(ticketId, id, callerId);
        return ResponseEntity.noContent().build();
    }
}
