package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.*;
import io.snortexware.sisflow.entities.TicketPriorityConfig;
import io.snortexware.sisflow.entities.TicketStatusConfig;
import io.snortexware.sisflow.entities.TicketTypeConfig;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.ParametricConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TicketConfigController extends BaseController {

    private final ParametricConfigService service;
    private final AuthorizationService authorizationService;

    @Override
    protected AuthorizationService authorizationService() { return authorizationService; }

    // ── Statuses ─────────────────────────────────────────────────────────────

    @GetMapping("/ticket-statuses")
    public ResponseEntity<List<TicketStatusConfig>> listStatuses(@AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(service.listStatuses());
    }

    @PostMapping("/ticket-statuses")
    public ResponseEntity<TicketStatusConfig> createStatus(@Valid @RequestBody CreateTicketStatusRequest req,
                                                           @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createStatus(req));
    }

    @PutMapping("/ticket-statuses/{id}")
    public ResponseEntity<TicketStatusConfig> updateStatus(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateTicketStatusRequest req,
                                                           @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(service.updateStatus(id, req));
    }

    @DeleteMapping("/ticket-statuses/{id}")
    public ResponseEntity<Void> deleteStatus(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        service.deleteStatus(id);
        return ResponseEntity.noContent().build();
    }

    // ── Priorities ────────────────────────────────────────────────────────────

    @GetMapping("/ticket-priorities")
    public ResponseEntity<List<TicketPriorityConfig>> listPriorities(@AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(service.listPriorities());
    }

    @PostMapping("/ticket-priorities")
    public ResponseEntity<TicketPriorityConfig> createPriority(@Valid @RequestBody CreateTicketPriorityRequest req,
                                                               @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPriority(req));
    }

    @PutMapping("/ticket-priorities/{id}")
    public ResponseEntity<TicketPriorityConfig> updatePriority(@PathVariable UUID id,
                                                               @Valid @RequestBody UpdateTicketPriorityRequest req,
                                                               @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(service.updatePriority(id, req));
    }

    @DeleteMapping("/ticket-priorities/{id}")
    public ResponseEntity<Void> deletePriority(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        service.deletePriority(id);
        return ResponseEntity.noContent().build();
    }

    // ── Types ─────────────────────────────────────────────────────────────────

    @GetMapping("/ticket-types")
    public ResponseEntity<List<TicketTypeConfig>> listTypes(@AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(service.listTypes());
    }

    @PostMapping("/ticket-types")
    public ResponseEntity<TicketTypeConfig> createType(@Valid @RequestBody CreateTicketTypeRequest req,
                                                       @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createType(req));
    }

    @PutMapping("/ticket-types/{id}")
    public ResponseEntity<TicketTypeConfig> updateType(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateTicketTypeRequest req,
                                                       @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(service.updateType(id, req));
    }

    @DeleteMapping("/ticket-types/{id}")
    public ResponseEntity<Void> deleteType(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        service.deleteType(id);
        return ResponseEntity.noContent().build();
    }
}
