package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.Permission;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.PermissionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

/**
 * REST controller for role-permission mapping endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/roles/{roleId}/permissions")
public class RolePermissionController {

    private final PermissionService permissionService;
    private final AuthorizationService authorizationService;

    public RolePermissionController(PermissionService permissionService, AuthorizationService authorizationService) {
        this.permissionService = permissionService;
        this.authorizationService = authorizationService;
    }

    /**
     * Grant a permission to a role.
     * POST /api/v1/roles/{roleId}/permissions/{permissionId}
     */
    @PostMapping("/{permissionId}")
    @Transactional
    public ResponseEntity<Void> grantPermissionToRole(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can manage role permissions
        try {
            authorizationService.validateCanManageRoles(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Granting permission {} to role {}", permissionId, roleId);
        permissionService.grantPermissionToRole(roleId, permissionId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Revoke a permission from a role.
     * DELETE /api/v1/roles/{roleId}/permissions/{permissionId}
     */
    @DeleteMapping("/{permissionId}")
    @Transactional
    public ResponseEntity<Void> revokePermissionFromRole(
            @PathVariable UUID roleId,
            @PathVariable UUID permissionId,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can manage role permissions
        try {
            authorizationService.validateCanManageRoles(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Revoking permission {} from role {}", permissionId, roleId);
        permissionService.revokePermissionFromRole(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all permissions for a role.
     * GET /api/v1/roles/{roleId}/permissions
     */
    @GetMapping
    public ResponseEntity<Set<Permission>> getRolePermissions(
            @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view role permissions
        try {
            authorizationService.validateCanManageRoles(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Getting permissions for role: {}", roleId);
        Set<Permission> permissions = permissionService.getRolePermissions(roleId);
        return ResponseEntity.ok(permissions);
    }
}
