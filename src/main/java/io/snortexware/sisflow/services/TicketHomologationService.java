package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.AssignHomologatorRequest;
import io.snortexware.sisflow.dto.HomologationDecisionRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketHomologation;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketHomologationRepository;
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
public class TicketHomologationService {

    private final TicketHomologationRepository ticketHomologationRepository;
    private final TicketRepository ticketRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthorizationService authorizationService;

    public List<TicketHomologation> list(UUID ticketId, UUID callerId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        return ticketHomologationRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    @Transactional
    public TicketHomologation assign(UUID ticketId, UUID callerId, AssignHomologatorRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);

        UserProfile user = userProfileRepository.findById(request.getUserId()).orElseThrow(AppException::notFound);
        if (ticketHomologationRepository.existsByTicketIdAndUserId(ticketId, request.getUserId())) {
            throw AppException.conflict();
        }

        TicketHomologation homologation = TicketHomologation.builder()
                .ticket(ticket)
                .user(user)
                .status(TicketHomologation.HomologationStatus.pending)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        return ticketHomologationRepository.save(homologation);
    }

    @Transactional
    public TicketHomologation decide(UUID ticketId, UUID id, UUID callerId, HomologationDecisionRequest request) {
        TicketHomologation homologation = ticketHomologationRepository.findById(id).orElseThrow(AppException::notFound);
        if (!homologation.getTicket().getId().equals(ticketId)) throw AppException.notFound();

        authorizationService.validateCanViewTicket(callerId, homologation.getTicket());
        if (!homologation.getUser().getId().equals(callerId)
                && !authorizationService.isModeratorOrAbove(callerId)) {
            throw AppException.forbidden();
        }

        homologation.setStatus(request.getStatus());
        homologation.setComment(request.getComment());
        homologation.setUpdatedAt(OffsetDateTime.now());
        return ticketHomologationRepository.save(homologation);
    }

    @Transactional
    public void remove(UUID ticketId, UUID id, UUID callerId) {
        TicketHomologation homologation = ticketHomologationRepository.findById(id).orElseThrow(AppException::notFound);
        if (!homologation.getTicket().getId().equals(ticketId)) throw AppException.notFound();

        authorizationService.validateCanViewTicket(callerId, homologation.getTicket());
        ticketHomologationRepository.delete(homologation);
    }
}
