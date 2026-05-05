package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateTicketRequest;
import io.snortexware.sisflow.dto.TransferTicketRequest;
import io.snortexware.sisflow.dto.UpdateTicketRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;

    public TicketController(TicketService ticketService, TicketRepository ticketRepository,
                             AuthorizationService authorizationService, TenantContext tenantContext) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.authorizationService = authorizationService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/check-code")
    public ResponseEntity<Boolean> checkCode(
            @RequestParam Long code,
            @RequestParam(required = false) UUID excludeId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        UUID tenantId = tenantContext.getCurrentTenant();
        boolean taken;
        if (tenantId != null) {
            taken = excludeId != null
                    ? ticketRepository.existsByCodeAndIdNotAndCustomer_Tenant_Id(code, excludeId, tenantId)
                    : ticketRepository.existsByCodeAndCustomer_Tenant_Id(code, tenantId);
        } else {
            taken = excludeId != null
                    ? ticketRepository.existsByCodeAndIdNot(code, excludeId)
                    : ticketRepository.existsByCode(code);
        }
        return ResponseEntity.ok(!taken);
    }

    @PostMapping
    public ResponseEntity<Ticket> create(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UUID callerId
    ) {
        Ticket ticket = ticketService.createTicket(request, callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getTickets(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        UUID tenantId = tenantContext.getCurrentTenant();
        List<Ticket> tickets;

        if (tenantId != null) {
            // Tenant-scoped: moderators see all tenant tickets; others see only their own
            if (authorizationService.isModeratorOrAbove(callerId)) {
                tickets = ticketRepository.findByTenantId(tenantId);
            } else {
                tickets = ticketRepository.findByTenantIdAndUserId(tenantId, callerId);
            }
        } else {
            // system_admin: no tenant restriction, but still scope to user unless admin
            if (authorizationService.isModeratorOrAbove(callerId)) {
                tickets = ticketRepository.findAll();
            } else {
                tickets = ticketRepository.findByUserId(callerId);
            }
        }

        return ResponseEntity.ok(tickets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        UUID tenantId = tenantContext.getCurrentTenant();
        Ticket ticket;

        if (tenantId != null) {
            ticket = ticketRepository.findByIdAndTenantId(id, tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        } else {
            ticket = ticketRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        }

        // Non-moderators can only view tickets they created or are assigned to
        if (!authorizationService.isModeratorOrAbove(callerId)) {
            boolean isOwner = (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(callerId))
                    || (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(callerId));
            if (!isOwner) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/my-queue")
    public ResponseEntity<List<Ticket>> getMyQueue(@AuthenticationPrincipal UUID callerId) {
        return ResponseEntity.ok(ticketService.getMyQueue(callerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UUID callerId
    ) {
        Ticket ticket = ticketService.updateTicket(id, request, callerId);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<Ticket> transfer(
            @PathVariable UUID id,
            @Valid @RequestBody TransferTicketRequest request,
            @AuthenticationPrincipal UUID callerId
    ) {
        Ticket ticket = ticketService.transfer(id, callerId, request);
        return ResponseEntity.ok(ticket);
    }
}
