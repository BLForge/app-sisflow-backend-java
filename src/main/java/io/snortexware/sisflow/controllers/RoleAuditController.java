package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.AuditEvent;
import io.snortexware.sisflow.repositories.AuditEventRepository;
import io.snortexware.sisflow.security.annotations.RequirePermission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for audit event endpoints related to roles and permissions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audit")
public class RoleAuditController {

    private final AuditEventRepository auditEventRepository;

    public RoleAuditController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    /**
     * List audit events with optional filtering.
     * GET /api/v1/audit/events
     */
    @GetMapping("/events")
    @RequirePermission("system:read")
    public ResponseEntity<Page<AuditEvent>> listAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        
        log.info("Listing audit events: page={}, size={}, userId={}, action={}", page, size, userId, action);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        // For now, return all events with pagination
        // This can be extended to support filtering by userId, action, date range
        Page<AuditEvent> events = auditEventRepository.findAll(pageable);
        
        return ResponseEntity.ok(events);
    }

    /**
     * Get audit events for a specific user.
     * GET /api/v1/audit/events/user/{userId}
     */
    @GetMapping("/events/user/{userId}")
    @RequirePermission("system:read")
    public ResponseEntity<List<AuditEvent>> getAuditEventsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting audit events for user: {}", userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditEvent> events = auditEventRepository.findByActorId(userId, pageable);
        
        return ResponseEntity.ok(events.getContent());
    }

    /**
     * Get audit events by action type.
     * GET /api/v1/audit/events/action/{action}
     */
    @GetMapping("/events/action/{action}")
    @RequirePermission("system:read")
    public ResponseEntity<List<AuditEvent>> getAuditEventsByAction(
            @PathVariable String action,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("Getting audit events for action: {}", action);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditEvent> events = auditEventRepository.findByAction(action, pageable);
        
        return ResponseEntity.ok(events.getContent());
    }
}
