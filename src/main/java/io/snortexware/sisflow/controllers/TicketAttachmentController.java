package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateAttachmentRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TicketAttachment;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketAttachmentRepository;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
    public ResponseEntity<List<TicketAttachment>> list(
            @PathVariable UUID ticketId,
            @AuthenticationPrincipal UUID callerId
    ) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // SECURITY: Verify user has access to this ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        
        try {
            authorizationService.validateCanViewTicket(callerId, ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(attachmentRepository.findByTicketIdOrderByCreatedAtDesc(ticketId));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TicketAttachment> create(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CreateAttachmentRequest request,
            @AuthenticationPrincipal UUID callerId) {

        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        // SECURITY: Verify user has access to this ticket
        try {
            authorizationService.validateCanViewTicket(callerId, ticket);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        UserProfile uploader = userProfileRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // SECURITY: Validate file URL to prevent malicious uploads
        if (request.getFileUrl() == null || request.getFileUrl().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File URL is required");
        }
        
        // SECURITY: Validate file name to prevent path traversal
        if (request.getFileName() == null || request.getFileName().contains("..") || 
            request.getFileName().contains("/") || request.getFileName().contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
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

        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentRepository.save(attachment));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(
            @PathVariable UUID ticketId, 
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        TicketAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        
        if (!attachment.getTicket().getId().equals(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        
        // SECURITY: Verify user has access to this ticket
        try {
            authorizationService.validateCanViewTicket(callerId, attachment.getTicket());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // SECURITY: Only the uploader or moderators+ can delete attachments
        if (!attachment.getUploadedBy().getId().equals(callerId) && 
            !authorizationService.isModeratorOrAbove(callerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        attachmentRepository.delete(attachment);
        return ResponseEntity.noContent().build();
    }
}
