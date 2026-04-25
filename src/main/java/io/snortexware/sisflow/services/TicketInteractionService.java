package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateInteractionRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketInteraction;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketInteractionRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
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
public class TicketInteractionService {

    private final TicketInteractionRepository ticketInteractionRepository;
    private final TicketRepository ticketRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional
    public TicketInteraction post(UUID ticketId, UUID callerId, CreateInteractionRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        UserProfile user = userProfileRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User profile not found"));

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
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
        }

        if ("client".equals(callerRole)) {
            return ticketInteractionRepository.findByTicketIdAndIsInternal(ticketId, false);
        }

        return ticketInteractionRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }
}
