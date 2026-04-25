package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateTicketRequest;
import io.snortexware.sisflow.dto.TransferTicketRequest;
import io.snortexware.sisflow.dto.UpdateTicketRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketRepository ticketRepository;
    private final AuthorizationService authorizationService;

    public TicketController(TicketService ticketService, TicketRepository ticketRepository, AuthorizationService authorizationService) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/check-code")
    public ResponseEntity<Boolean> checkCode(
            @RequestParam Long code, 
            @RequestParam(required = false) UUID excludeId,
            @AuthenticationPrincipal UUID callerId
    ) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        boolean available = excludeId != null
            ? !ticketRepository.existsByCodeAndIdNot(code, excludeId)
            : !ticketRepository.existsByCode(code);
        return ResponseEntity.ok(available);
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
    public ResponseEntity<List<Ticket>> getTickets(@AuthenticationPrincipal UUID callerId){
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Apply authorization filtering
            List<Ticket> allTickets = ticketRepository.findAll();
            
            // Filter tickets based on user access
            List<Ticket> filteredTickets = allTickets.stream()
                .filter(ticket -> canUserAccessTicket(ticket, callerId))
                .toList();
                
            return ResponseEntity.ok(filteredTickets);
        } catch (Exception e) {
            // If there's an error, return empty list for security
            return ResponseEntity.ok(List.of());
        }
    }
    
    private boolean canUserAccessTicket(Ticket ticket, UUID userId) {
        if (userId == null) {
            return false;
        }
        
        try {
            // Admins and moderators can see all tickets
            if (authorizationService.isModeratorOrAbove(userId)) {
                return true;
            }
            
            // User can see tickets they created or are assigned to
            if (ticket.getCreatedBy() != null && ticket.getCreatedBy().getId().equals(userId)) {
                return true;
            }
            
            if (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(userId)) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
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
