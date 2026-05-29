package io.snortexware.sisflow.services;

import io.snortexware.sisflow.entities.AuditEvent;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.repositories.AuditEventRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;
    private final TicketRepository ticketRepository;
    private final TenantContext tenantContext;

    public List<AuditEvent> listByTicket(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);

        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant != null && (ticket.getCustomer() == null
                || ticket.getCustomer().getTenant() == null
                || !ticket.getCustomer().getTenant().getId().equals(callerTenant))) {
            throw AppException.notFound();
        }

        return auditEventRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }
}
