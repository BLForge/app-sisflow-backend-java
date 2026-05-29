package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.KnowledgeBase;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<List<KnowledgeBase>> listForTicket(@PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        Ticket ticket = ticketRepository.findById(id).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        return ResponseEntity.ok(knowledgeBaseService.listForTicket(id));
    }

    @PostMapping("/{articleId}")
    public ResponseEntity<Void> linkToTicket(@PathVariable UUID id, @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageKnowledgeBase(callerId);
        Ticket ticket = ticketRepository.findById(id).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        knowledgeBaseService.linkToTicket(id, articleId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<Void> unlinkFromTicket(@PathVariable UUID id, @PathVariable UUID articleId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageKnowledgeBase(callerId);
        Ticket ticket = ticketRepository.findById(id).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        knowledgeBaseService.unlinkFromTicket(id, articleId);
        return ResponseEntity.noContent().build();
    }
}
