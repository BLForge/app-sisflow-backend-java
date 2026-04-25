package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.AuditEvent;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.repositories.AuditEventRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{id}/audit")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventRepository auditEventRepository;
    private final TicketRepository ticketRepository;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<AuditEvent>> list(
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

        return ResponseEntity.ok(auditEventRepository.findByTicketIdOrderByCreatedAtDesc(id));
    }
}
