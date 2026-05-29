package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateInteractionRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketInteraction;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketInteractionRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketInteractionService {

    private final TicketInteractionRepository ticketInteractionRepository;
    private final TicketRepository ticketRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthorizationService authorizationService;

    @Transactional
    public TicketInteraction post(UUID ticketId, UUID callerId, CreateInteractionRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(AppException::notFound);

        UserProfile user = userProfileRepository.findById(callerId)
                .orElseThrow(AppException::unauthorized);

        TicketInteraction interaction = TicketInteraction.builder()
                .ticket(ticket)
                .user(user)
                .message(request.getMessage())
                .isInternal(request.isInternal())
                .createdAt(OffsetDateTime.now())
                .build();

        return ticketInteractionRepository.save(interaction);
    }

    public List<TicketInteraction> list(UUID ticketId, String callerRole) {
        if (!ticketRepository.existsById(ticketId))
            throw AppException.notFound();

        if ("client".equals(callerRole)) {
            return ticketInteractionRepository.findByTicketIdAndIsInternal(ticketId, false);
        }

        return ticketInteractionRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public List<TicketInteraction> listForCaller(UUID ticketId, UUID callerId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);

        UserProfile caller = userProfileRepository.findById(callerId).orElseThrow(AppException::unauthorized);
        return list(ticketId, caller.getRole().name());
    }

    @Transactional
    public TicketInteraction postForCaller(UUID ticketId, UUID callerId, CreateInteractionRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        return post(ticketId, callerId, request);
    }
}
