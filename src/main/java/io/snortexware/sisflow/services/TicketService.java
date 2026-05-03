package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateInteractionRequest;
import io.snortexware.sisflow.dto.CreateTicketRequest;
import io.snortexware.sisflow.dto.TransferTicketRequest;
import io.snortexware.sisflow.dto.UpdateTicketRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.Sla;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketStatusConfig;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.SlaRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.TicketStatusConfigRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserProfileRepository userProfileRepository;
    private final CustomerRepository customerRepository;
    private final SlaRepository slaRepository;
    private final TicketStatusConfigRepository statusRepository;
    private final AuditService auditService;
    private final TicketInteractionService ticketInteractionService;

    @Transactional
    public Ticket createTicket(CreateTicketRequest req, UUID callerId) {

        UserProfile caller = userProfileRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User profile not found"));

        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));

        Sla sla = slaRepository.findById(req.getSlaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SLA not found"));

        // Find default "open" status or create one
        TicketStatusConfig defaultStatus = statusRepository.findAll().stream()
                .filter(s -> "open".equalsIgnoreCase(s.getName()) || "aberto".equalsIgnoreCase(s.getName()))
                .findFirst()
                .orElseGet(() -> {
                    TicketStatusConfig newStatus = TicketStatusConfig.builder()
                            .name("Aberto")
                            .color("#3B82F6")
                            .isClosed(false)
                            .build();
                    return statusRepository.save(newStatus);
                });

        Ticket ticket = Ticket.builder()
                .code(ticketRepository.findMaxCode() + 1)
                .title(req.getTitle())
                .description(req.getDescription())
                .status(defaultStatus)
                .priority(req.getPriority())
                .type(req.getType())
                .customer(customer)
                .createdBy(caller)
                .sla(sla)
                .createdAt(OffsetDateTime.now())
                .build();

        Ticket saved = ticketRepository.save(ticket);
        auditService.record(saved.getId(), callerId, AuditService.TICKET_CREATED, null, saved.getId().toString());
        return saved;
    }

    public List<Ticket> getMyQueue(UUID callerId) {
        return ticketRepository.findMyQueue(callerId);
    }

    @Transactional
    public Ticket updateTicket(UUID ticketId, UpdateTicketRequest req, UUID callerId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        Customer customer = customerRepository.findById(req.getCustomerId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Customer not found"));

        Sla sla = slaRepository.findById(req.getSlaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "SLA not found"));

        TicketStatusConfig status = statusRepository.findById(req.getStatusId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status not found"));

        // Capture old values before mutation
        TicketStatusConfig oldStatus = ticket.getStatus();
        Ticket.Priority oldPriority = ticket.getPriority();
        Ticket.TicketType oldType = ticket.getType();
        UUID oldSlaId = ticket.getSla() != null ? ticket.getSla().getId() : null;
        UUID oldAssignedToId = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null;

        // Validate custom code if provided
        if (req.getCode() != null && !req.getCode().equals(ticket.getCode())) {
            if (ticketRepository.existsByCodeAndIdNot(req.getCode(), ticketId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ticket code already in use");
            }
            ticket.setCode(req.getCode());
        }

        ticket.setTitle(req.getTitle());
        ticket.setDescription(req.getDescription());
        ticket.setPrivateNotes(req.getPrivateNotes());
        ticket.setPriority(req.getPriority());
        ticket.setType(req.getType());
        ticket.setStatus(status);
        ticket.setCustomer(customer);
        ticket.setSla(sla);

        Ticket saved = ticketRepository.save(ticket);

        // Record audit events for changed fields
        if (oldStatus != null && !oldStatus.getId().equals(req.getStatusId())) {
            auditService.record(ticketId, callerId, AuditService.STATUS_CHANGED,
                    oldStatus.getName(), status.getName());
        }
        if (oldPriority != req.getPriority()) {
            auditService.record(ticketId, callerId, AuditService.PRIORITY_CHANGED,
                    oldPriority != null ? oldPriority.name() : null, req.getPriority().name());
        }
        if (oldType != req.getType()) {
            auditService.record(ticketId, callerId, AuditService.TYPE_CHANGED,
                    oldType != null ? oldType.name() : null, req.getType().name());
        }
        if (!req.getSlaId().equals(oldSlaId)) {
            auditService.record(ticketId, callerId, AuditService.SLA_CHANGED,
                    oldSlaId != null ? oldSlaId.toString() : null, req.getSlaId().toString());
        }
        UUID newAssignedToId = saved.getAssignedTo() != null ? saved.getAssignedTo().getId() : null;
        if (!java.util.Objects.equals(oldAssignedToId, newAssignedToId)) {
            auditService.record(ticketId, callerId, AuditService.ASSIGNED_TO_CHANGED,
                    oldAssignedToId != null ? oldAssignedToId.toString() : null,
                    newAssignedToId != null ? newAssignedToId.toString() : null);
        }

        return saved;
    }

    @Transactional
    public Ticket transfer(UUID ticketId, UUID callerId, TransferTicketRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        UserProfile caller = userProfileRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User profile not found"));

        UserProfile targetAgent = userProfileRepository.findById(request.getTargetAgentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target agent not found"));

        if (ticket.getGroup() != null && caller.getRole() != UserProfile.Role.admin) {
            boolean isMember = ticket.getGroup().getMembers().stream()
                    .anyMatch(m -> m.getId().equals(targetAgent.getId()));
            if (!isMember) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Target agent is not a member of the ticket's group");
            }
        }

        UUID oldAssignedToId = ticket.getAssignedTo() != null ? ticket.getAssignedTo().getId() : null;

        ticket.setAssignedTo(targetAgent);
        Ticket saved = ticketRepository.save(ticket);

        auditService.record(ticketId, callerId, AuditService.TRANSFERRED,
                oldAssignedToId != null ? oldAssignedToId.toString() : null,
                request.getTargetAgentId().toString());

        if (request.getReason() != null && !request.getReason().isBlank()) {
            CreateInteractionRequest interactionRequest = new CreateInteractionRequest();
            interactionRequest.setMessage(request.getReason());
            interactionRequest.setInternal(true);
            ticketInteractionService.post(ticketId, callerId, interactionRequest);
        }

        return saved;
    }
}
