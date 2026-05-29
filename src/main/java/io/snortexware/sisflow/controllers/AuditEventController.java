package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.AuditEvent;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.repositories.AuditEventRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{id}/audit")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventRepository auditEventRepository;
    private final TicketRepository ticketRepository;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;

    @GetMapping
    public ResponseEntity<List<AuditEvent>> list(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();

        Ticket ticket = ticketRepository.findById(id).orElseThrow(AppException::notFound);

        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant != null && (ticket.getCustomer() == null
                || ticket.getCustomer().getTenant() == null
                || !ticket.getCustomer().getTenant().getId().equals(callerTenant)))
            throw AppException.notFound();

        return ResponseEntity.ok(auditEventRepository.findByTicketIdOrderByCreatedAtDesc(id));
    }
}
