package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.KnowledgeBase;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{id}/knowledge-base")
@RequiredArgsConstructor
public class TicketKnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final TicketRepository ticketRepository;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<KnowledgeBase>> listForTicket(
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
        
        return ResponseEntity.ok(knowledgeBaseService.listForTicket(id));
    }

    @PostMapping("/{articleId}")
    public ResponseEntity<Void> linkToTicket(
            @PathVariable UUID id,
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID callerId
    ) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // SECURITY: Only developers and above can manage knowledge base
        try {
            authorizationService.validateCanManageKnowledgeBase(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // SECURITY: Verify ticket exists and user has access
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        
        try {
            authorizationService.validateCanViewTicket(callerId, ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        knowledgeBaseService.linkToTicket(id, articleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<Void> unlinkFromTicket(
            @PathVariable UUID id,
            @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID callerId
    ) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // SECURITY: Only developers and above can manage knowledge base
        try {
            authorizationService.validateCanManageKnowledgeBase(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // SECURITY: Verify ticket exists and user has access
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        
        try {
            authorizationService.validateCanViewTicket(callerId, ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        knowledgeBaseService.unlinkFromTicket(id, articleId);
        return ResponseEntity.noContent().build();
    }
}
