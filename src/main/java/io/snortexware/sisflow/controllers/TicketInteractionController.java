package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateInteractionRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketInteraction;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.TicketInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<TicketInteraction>> list(@PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();

        Ticket ticket = ticketRepository.findById(id).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);

        UserProfile caller = userProfileRepository.findById(callerId).orElseThrow(AppException::unauthorized);
        return ResponseEntity.ok(ticketInteractionService.list(id, caller.getRole().name()));
    }

    @PostMapping
    public ResponseEntity<TicketInteraction> post(@PathVariable UUID id,
            @Valid @RequestBody CreateInteractionRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();

        Ticket ticket = ticketRepository.findById(id).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);

        return ResponseEntity.status(HttpStatus.CREATED).body(ticketInteractionService.post(id, callerId, request));
    }
}
