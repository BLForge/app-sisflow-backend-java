package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.TenantRegistrationRequest;
import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final TenantRepository tenantRepository;
    private final AuthorizationService authorizationService;

    @PostMapping("/tenants/register")
    public ResponseEntity<Void> register(@Valid @RequestBody TenantRegistrationRequest req) {
        tenantService.register(req);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/tenants")
    public ResponseEntity<List<Tenant>> listTenants(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isAdminOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(tenantRepository.findAll());
    }
}
