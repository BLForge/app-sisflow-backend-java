package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.AssignHomologatorRequest;
import io.snortexware.sisflow.dto.HomologationDecisionRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketHomologation;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketHomologationRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<TicketHomologation>> list(@PathVariable UUID ticketId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        return ResponseEntity.ok(homologationRepository.findByTicketIdOrderByCreatedAtAsc(ticketId));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TicketHomologation> assign(@PathVariable UUID ticketId,
            @Valid @RequestBody AssignHomologatorRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();

        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);

        UserProfile user = userProfileRepository.findById(request.getUserId()).orElseThrow(AppException::notFound);
        if (homologationRepository.existsByTicketIdAndUserId(ticketId, request.getUserId()))
            throw AppException.conflict();

        TicketHomologation h = TicketHomologation.builder()
                .ticket(ticket).user(user)
                .status(TicketHomologation.HomologationStatus.pending)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(homologationRepository.save(h));
    }

    @PutMapping("/{id}/decision")
    @Transactional
    public ResponseEntity<TicketHomologation> decide(@PathVariable UUID ticketId, @PathVariable UUID id,
            @Valid @RequestBody HomologationDecisionRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();

        TicketHomologation h = homologationRepository.findById(id).orElseThrow(AppException::notFound);
        if (!h.getTicket().getId().equals(ticketId)) throw AppException.notFound();

        authorizationService.validateCanViewTicket(callerId, h.getTicket());

        if (!h.getUser().getId().equals(callerId) && !authorizationService.isModeratorOrAbove(callerId))
            throw AppException.forbidden();

        h.setStatus(request.getStatus());
        h.setComment(request.getComment());
        h.setUpdatedAt(OffsetDateTime.now());

        return ResponseEntity.ok(homologationRepository.save(h));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> remove(@PathVariable UUID ticketId, @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();

        TicketHomologation h = homologationRepository.findById(id).orElseThrow(AppException::notFound);
        if (!h.getTicket().getId().equals(ticketId)) throw AppException.notFound();

        authorizationService.validateCanViewTicket(callerId, h.getTicket());
        homologationRepository.delete(h);
        return ResponseEntity.noContent().build();
    }
}
