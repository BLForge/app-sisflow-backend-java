package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateInteractionRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketInteraction;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.TicketInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{id}/interactions")
@RequiredArgsConstructor
public class TicketInteractionController {

    private final TicketInteractionService ticketInteractionService;
    private final UserProfileRepository userProfileRepository;
    private final TicketRepository ticketRepository;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<TicketInteraction>> list(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId
    ) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Verify user has access to this ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        
        try {
            authorizationService.validateCanViewTicket(callerId, ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserProfile caller = userProfileRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User profile not found"));

        String callerRole = caller.getRole().name();
        return ResponseEntity.ok(ticketInteractionService.list(id, callerRole));
    }

    @PostMapping
    public ResponseEntity<TicketInteraction> post(
            @PathVariable UUID id,
            @Valid @RequestBody CreateInteractionRequest request,
            @AuthenticationPrincipal UUID callerId
    ) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Verify user has access to this ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        
        try {
            authorizationService.validateCanViewTicket(callerId, ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TicketInteraction interaction = ticketInteractionService.post(id, callerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(interaction);
    }
}
