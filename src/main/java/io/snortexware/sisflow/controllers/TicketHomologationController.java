package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.AssignHomologatorRequest;
import io.snortexware.sisflow.dto.HomologationDecisionRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketHomologation;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketHomologationRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{ticketId}/homologations")
@RequiredArgsConstructor
public class TicketHomologationController {

    private final TicketHomologationRepository homologationRepository;
    private final TicketRepository ticketRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<TicketHomologation>> list(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UUID callerId
    ) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // SECURITY: Verify user has access to this ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        
        try {
            authorizationService.validateCanViewTicket(callerId, ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(homologationRepository.findByTicketIdOrderByCreatedAtAsc(ticketId));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TicketHomologation> assign(
            @PathVariable UUID ticketId,
            @Valid @RequestBody AssignHomologatorRequest request,
            @AuthenticationPrincipal UUID callerId) {

        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only moderators and above can assign homologators
        if (!authorizationService.isModeratorOrAbove(callerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        // SECURITY: Verify user has access to this ticket
        try {
            authorizationService.validateCanViewTicket(callerId, ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserProfile user = userProfileRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (homologationRepository.existsByTicketIdAndUserId(ticketId, request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already assigned as homologator");
        }

        TicketHomologation h = TicketHomologation.builder()
                .ticket(ticket)
                .user(user)
                .status(TicketHomologation.HomologationStatus.pending)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(homologationRepository.save(h));
    }

    @PutMapping("/{id}/decision")
    @Transactional
    public ResponseEntity<TicketHomologation> decide(
            @PathVariable UUID ticketId,
            @PathVariable UUID id,
            @Valid @RequestBody HomologationDecisionRequest request,
            @AuthenticationPrincipal UUID callerId) {

        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        TicketHomologation h = homologationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Homologation not found"));

        if (!h.getTicket().getId().equals(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Homologation not found");
        }

        // SECURITY: Verify user has access to this ticket
        try {
            authorizationService.validateCanViewTicket(callerId, h.getTicket());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // SECURITY: Only the assigned homologator or moderators+ can decide
        if (!h.getUser().getId().equals(callerId)) {
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned homologator or moderators can decide");
            }
        }

        h.setStatus(request.getStatus());
        h.setComment(request.getComment());
        h.setUpdatedAt(OffsetDateTime.now());

        return ResponseEntity.ok(homologationRepository.save(h));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> remove(
            @PathVariable UUID ticketId, 
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only moderators and above can remove homologations
        if (!authorizationService.isModeratorOrAbove(callerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        TicketHomologation h = homologationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Homologation not found"));
        
        if (!h.getTicket().getId().equals(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // SECURITY: Verify user has access to this ticket
        try {
            authorizationService.validateCanViewTicket(callerId, h.getTicket());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        homologationRepository.delete(h);
        return ResponseEntity.noContent().build();
    }
}
