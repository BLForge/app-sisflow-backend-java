package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.Permission;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles/{roleId}/permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final PermissionService permissionService;
    private final AuthorizationService authorizationService;

    @PostMapping("/{permissionId}")
    @Transactional
    public ResponseEntity<Void> grantPermissionToRole(@PathVariable UUID roleId, @PathVariable UUID permissionId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageRoles(callerId);
        permissionService.grantPermissionToRole(roleId, permissionId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{permissionId}")
    @Transactional
    public ResponseEntity<Void> revokePermissionFromRole(@PathVariable UUID roleId, @PathVariable UUID permissionId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageRoles(callerId);
        permissionService.revokePermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Set<Permission>> getRolePermissions(@PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageRoles(callerId);
        return ResponseEntity.ok(permissionService.getRolePermissions(roleId));
    }
}
