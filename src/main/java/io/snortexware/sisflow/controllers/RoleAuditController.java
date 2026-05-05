package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.AuditEvent;
import io.snortexware.sisflow.repositories.AuditEventRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.services.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/audit")
public class RoleAuditController {

    private final AuditEventRepository auditEventRepository;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;

    public RoleAuditController(AuditEventRepository auditEventRepository,
                                AuthorizationService authorizationService,
                                TenantContext tenantContext) {
        this.auditEventRepository = auditEventRepository;
        this.authorizationService = authorizationService;
        this.tenantContext = tenantContext;
    }

    @GetMapping("/events")
    public ResponseEntity<Page<AuditEvent>> listAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UUID callerId) {

        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authorizationService.isAdminOrAbove(callerId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
        UUID tenantId = tenantContext.getCurrentTenant();

        Page<AuditEvent> events = tenantId != null
                ? auditEventRepository.findByTicket_Customer_Tenant_Id(tenantId, pageable)
                : auditEventRepository.findAll(pageable);

        return ResponseEntity.ok(events);
    }
}
