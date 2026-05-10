package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateAttachmentRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketAttachment;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketAttachmentRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final TicketAttachmentRepository attachmentRepository;
    private final TicketRepository ticketRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<TicketAttachment>> list(@PathVariable UUID ticketId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        return ResponseEntity.ok(attachmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TicketAttachment> create(@PathVariable UUID ticketId,
            @Valid @RequestBody CreateAttachmentRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();

        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);

        UserProfile uploader = userProfileRepository.findById(callerId).orElseThrow(AppException::unauthorized);

        if (request.getFileUrl() == null || request.getFileUrl().isBlank()) throw AppException.badRequest();
        if (request.getFileName() == null || request.getFileName().contains("..")
                || request.getFileName().contains("/") || request.getFileName().contains("\\"))
            throw AppException.badRequest();

        TicketAttachment attachment = TicketAttachment.builder()
                .ticket(ticket).uploadedBy(uploader)
                .fileName(request.getFileName()).fileUrl(request.getFileUrl())
                .fileSize(request.getFileSize()).mimeType(request.getMimeType())
                .createdAt(OffsetDateTime.now()).build();

        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentRepository.save(attachment));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID ticketId, @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();

        TicketAttachment attachment = attachmentRepository.findById(id).orElseThrow(AppException::notFound);
        if (!attachment.getTicket().getId().equals(ticketId)) throw AppException.notFound();

        authorizationService.validateCanViewTicket(callerId, attachment.getTicket());

        if (!attachment.getUploadedBy().getId().equals(callerId)
                && !authorizationService.isModeratorOrAbove(callerId))
            throw AppException.forbidden();

        attachmentRepository.delete(attachment);
        return ResponseEntity.noContent().build();
    }
}
