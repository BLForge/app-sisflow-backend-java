package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.BulkRoleAssignmentRequest;
import io.snortexware.sisflow.entities.Permission;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.security.annotations.RequirePermission;
import io.snortexware.sisflow.services.PermissionService;
import io.snortexware.sisflow.services.RoleService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for user-role assignment endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}")
public class UserRoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;

    public UserRoleController(RoleService roleService, PermissionService permissionService) {
        this.roleService = roleService;
        this.permissionService = permissionService;
    }

    /**
     * Assign a role to a user.
     * POST /api/v1/users/{userId}/roles/{roleId}
     */
    @PostMapping("/roles/{roleId}")
    @Transactional
    @RequirePermission("user:update")
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        log.info("Assigning role {} to user {}", roleId, userId);
        roleService.assignRoleToUser(userId, roleId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Remove a role from a user.
     * DELETE /api/v1/users/{userId}/roles/{roleId}
     */
    @DeleteMapping("/roles/{roleId}")
    @Transactional
    @RequirePermission("user:update")
    public ResponseEntity<Void> removeRoleFromUser(
            @PathVariable UUID userId,
            @PathVariable UUID roleId) {
        log.info("Removing role {} from user {}", roleId, userId);
        roleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all roles for a user.
     * GET /api/v1/users/{userId}/roles
     */
    @GetMapping("/roles")
    @RequirePermission("user:read")
    public ResponseEntity<List<Role>> getUserRoles(@PathVariable UUID userId) {
        log.info("Getting roles for user: {}", userId);
        List<Role> roles = roleService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

    /**
     * Get all permissions for a user.
     * GET /api/v1/users/{userId}/permissions
     */
    @GetMapping("/permissions")
    @RequirePermission("user:read")
    public ResponseEntity<Set<Permission>> getUserPermissions(@PathVariable UUID userId) {
        log.info("Getting permissions for user: {}", userId);
        Set<Permission> permissions = permissionService.getUserPermissions(userId);
        return ResponseEntity.ok(permissions);
    }

    /**
     * Bulk assign a role to multiple users.
     * POST /api/v1/roles/bulk-assign
     */
    @PostMapping("/roles/bulk-assign")
    @Transactional
    @RequirePermission("user:update")
    public ResponseEntity<Void> bulkAssignRoles(@Valid @RequestBody BulkRoleAssignmentRequest request) {
        log.info("Bulk assigning role {} to {} users", request.getRoleId(), request.getUserIds().size());
        roleService.bulkAssignRoles(request.getUserIds(), request.getRoleId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
