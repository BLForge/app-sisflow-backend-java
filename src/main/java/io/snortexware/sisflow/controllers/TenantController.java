package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.TenantRegistrationRequest;
import io.snortexware.sisflow.dto.UpdateTenantBrandingRequest;
import io.snortexware.sisflow.entities.Tenant;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TenantController {

    private final TenantService tenantService;
    private final AuthorizationService authorizationService;
    private final TenantResolver tenantResolver;

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
        return ResponseEntity.ok(tenantService.updateBranding(callerId, req));
    }

    @GetMapping("/admin/tenants")
    public ResponseEntity<List<Tenant>> listTenants(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(tenantService.listTenants());
    }
}
