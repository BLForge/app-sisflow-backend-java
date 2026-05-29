package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.AuditEvent;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuditEventService;
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

    private final AuditEventService auditEventService;

    @GetMapping
    public ResponseEntity<List<AuditEvent>> list(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(auditEventService.listByTicket(id));
    }
}
