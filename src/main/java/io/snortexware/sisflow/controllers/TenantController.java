package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.TenantRegistrationRequest;
import io.snortexware.sisflow.dto.UpdateTenantBrandingRequest;
import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.TenantResolver;
import io.snortexware.sisflow.services.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TenantController {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;
    private final TenantResolver tenantResolver;

    private int getCurrentUserHierarchyLevel(UUID userId) {
        return authorizationService.getCurrentUserHierarchyLevel(userId);
    }

    @PostMapping("/tenants/register")
    public ResponseEntity<Void> register(@Valid @RequestBody TenantRegistrationRequest req) {
        tenantService.register(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tenants/branding")
    public ResponseEntity<Tenant> getBranding(HttpServletRequest request) {
        return tenantResolver.resolveFromRequest(request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.ok(Tenant.builder()
                        .name("SisFlow")
                        .build()));
    }

    @PatchMapping("/tenants/me")
    public ResponseEntity<Tenant> updateBranding(@Valid @RequestBody UpdateTenantBrandingRequest req,
                                                  @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();

        UUID tenantId = tenantContext.getCurrentTenant();
        if (tenantId == null) throw AppException.unauthorized();

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(AppException::notFound);
        if (req.getName() != null && !req.getName().isBlank()) {
            if (getCurrentUserHierarchyLevel(callerId) < 4) {
                throw AppException.forbidden();
            }
            if (req.getName().length() > 100) throw AppException.badRequest();
            tenant.setName(req.getName());
        }
        if (req.getLogoUrl() != null && !req.getLogoUrl().isBlank()) {
            if (!req.getLogoUrl().startsWith("/files/")) {
                throw AppException.badRequest();
            }
            tenant.setLogoUrl(req.getLogoUrl());
        }

        if (req.getLogoIconUrl() != null && !req.getLogoIconUrl().isBlank()) {
            if (!req.getLogoIconUrl().startsWith("/files/")) {
                throw AppException.badRequest();
            }
            tenant.setLogoIconUrl(req.getLogoIconUrl());
        }
        
        if (req.getBackgroundUrl() != null && !req.getBackgroundUrl().isBlank()) {
            if (!req.getBackgroundUrl().startsWith("/files/")) {
                throw AppException.badRequest();
            }
            tenant.setBackgroundUrl(req.getBackgroundUrl());
        }
        
        tenant.setUpdatedAt(OffsetDateTime.now());
        return ResponseEntity.ok(tenantRepository.save(tenant));
    }

    @GetMapping("/admin/tenants")
    public ResponseEntity<List<Tenant>> listTenants(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(tenantRepository.findAll());
    }
}
