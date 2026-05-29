package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateAttachmentRequest;
import io.snortexware.sisflow.entities.TicketAttachment;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.TicketAttachmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final TicketAttachmentService ticketAttachmentService;

    @GetMapping
    public ResponseEntity<List<TicketAttachment>> list(@PathVariable UUID ticketId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(ticketAttachmentService.list(ticketId, callerId));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<TicketAttachment> create(@PathVariable UUID ticketId,
            @Valid @RequestBody CreateAttachmentRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketAttachmentService.create(ticketId, callerId, request));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> delete(@PathVariable UUID ticketId, @PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        ticketAttachmentService.delete(ticketId, id, callerId);
        return ResponseEntity.noContent().build();
    }
}
