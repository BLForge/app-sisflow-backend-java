package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreatePermissionRequest;
import io.snortexware.sisflow.dto.UpdatePermissionRequest;
import io.snortexware.sisflow.entities.Permission;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.PermissionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for permission management endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;
    private final AuthorizationService authorizationService;

    public PermissionController(PermissionService permissionService, AuthorizationService authorizationService) {
        this.permissionService = permissionService;
        this.authorizationService = authorizationService;
    }

    /**
     * Create a new permission.
     * POST /api/v1/permissions
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Permission> createPermission(
            @Valid @RequestBody CreatePermissionRequest request,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can create permissions
        try {
            authorizationService.validateCanManagePermissions(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Creating new permission: {}", request.getCode());
        Permission permission = permissionService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(permission);
    }

    /**
     * Update an existing permission.
     * PUT /api/v1/permissions/{permissionId}
     */
    @PutMapping("/{permissionId}")
    @Transactional
    public ResponseEntity<Permission> updatePermission(
            @PathVariable UUID permissionId,
            @Valid @RequestBody UpdatePermissionRequest request,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can update permissions
        try {
            authorizationService.validateCanManagePermissions(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Updating permission: {}", permissionId);
        Permission permission = permissionService.updatePermission(permissionId, request);
        return ResponseEntity.ok(permission);
    }

    /**
     * Delete a permission.
     * DELETE /api/v1/permissions/{permissionId}
     */
    @DeleteMapping("/{permissionId}")
    @Transactional
    public ResponseEntity<Void> deletePermission(
            @PathVariable UUID permissionId,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can delete permissions
        try {
            authorizationService.validateCanManagePermissions(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Deleting permission: {}", permissionId);
        permissionService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get a permission by ID.
     * GET /api/v1/permissions/{permissionId}
     */
    @GetMapping("/{permissionId}")
    public ResponseEntity<Permission> getPermissionById(
            @PathVariable UUID permissionId,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view permissions
        try {
            authorizationService.validateCanManagePermissions(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Getting permission: {}", permissionId);
        Permission permission = permissionService.getPermissionById(permissionId);
        return ResponseEntity.ok(permission);
    }

    /**
     * List all permissions.
     * GET /api/v1/permissions
     */
    @GetMapping
    public ResponseEntity<List<Permission>> getAllPermissions(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view permissions
        try {
            authorizationService.validateCanManagePermissions(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Listing all permissions");
        List<Permission> permissions = permissionService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    /**
     * Get permissions by category.
     * GET /api/v1/permissions/category/{category}
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Permission>> getPermissionsByCategory(
            @PathVariable String category,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view permissions
        try {
            authorizationService.validateCanManagePermissions(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Getting permissions by category: {}", category);
        List<Permission> permissions = permissionService.getPermissionsByCategory(category);
        return ResponseEntity.ok(permissions);
    }
}
