package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateAttachmentRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketAttachment;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketAttachmentRepository;
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
public class TicketAttachmentService {

    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TicketRepository ticketRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthorizationService authorizationService;

    public List<TicketAttachment> list(UUID ticketId, UUID callerId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        return ticketAttachmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId);
    }

    @Transactional
    public TicketAttachment create(UUID ticketId, UUID callerId, CreateAttachmentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);

        UserProfile uploader = userProfileRepository.findById(callerId).orElseThrow(AppException::unauthorized);
        if (request.getFileUrl() == null || request.getFileUrl().isBlank()) throw AppException.badRequest();
        if (request.getFileName() == null || request.getFileName().contains("..")
                || request.getFileName().contains("/") || request.getFileName().contains("\\")) {
            throw AppException.badRequest();
        }

        TicketAttachment attachment = TicketAttachment.builder()
                .ticket(ticket)
                .uploadedBy(uploader)
                .fileName(request.getFileName())
                .fileUrl(request.getFileUrl())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .createdAt(OffsetDateTime.now())
                .build();

        return ticketAttachmentRepository.save(attachment);
    }

    @Transactional
    public void delete(UUID ticketId, UUID id, UUID callerId) {
        TicketAttachment attachment = ticketAttachmentRepository.findById(id).orElseThrow(AppException::notFound);
        if (!attachment.getTicket().getId().equals(ticketId)) throw AppException.notFound();

        authorizationService.validateCanViewTicket(callerId, attachment.getTicket());
        if (!attachment.getUploadedBy().getId().equals(callerId)
                && !authorizationService.isModeratorOrAbove(callerId)) {
            throw AppException.forbidden();
        }

        ticketAttachmentRepository.delete(attachment);
    }
}
