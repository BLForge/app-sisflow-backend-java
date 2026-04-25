package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateRoleRequest;
import io.snortexware.sisflow.dto.UpdateRoleRequest;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.RoleService;
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
 * REST controller for role management endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/roles")
public class RoleController {

    private final RoleService roleService;
    private final AuthorizationService authorizationService;

    public RoleController(RoleService roleService, AuthorizationService authorizationService) {
        this.roleService = roleService;
        this.authorizationService = authorizationService;
    }

    /**
     * Create a new role.
     * POST /api/v1/roles
     */
    @PostMapping
    @Transactional
    public ResponseEntity<Role> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can create roles
        try {
            authorizationService.validateCanManageRoles(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Creating new role: {}", request.getCode());
        Role role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(role);
    }

    /**
     * Update an existing role.
     * PUT /api/v1/roles/{roleId}
     */
    @PutMapping("/{roleId}")
    @Transactional
    public ResponseEntity<Role> updateRole(
            @PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can update roles
        try {
            authorizationService.validateCanManageRoles(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Updating role: {}", roleId);
        Role role = roleService.updateRole(roleId, request);
        return ResponseEntity.ok(role);
    }

    /**
     * Delete a role.
     * DELETE /api/v1/roles/{roleId}
     */
    @DeleteMapping("/{roleId}")
    @Transactional
    public ResponseEntity<Void> deleteRole(
            @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can delete roles
        try {
            authorizationService.validateCanManageRoles(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Deleting role: {}", roleId);
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get a role by ID.
     * GET /api/v1/roles/{roleId}
     */
    @GetMapping("/{roleId}")
    public ResponseEntity<Role> getRoleById(
            @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view roles
        try {
            authorizationService.validateCanManageRoles(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Getting role: {}", roleId);
        Role role = roleService.getRoleById(roleId);
        return ResponseEntity.ok(role);
    }

    /**
     * List all roles.
     * GET /api/v1/roles
     */
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view roles
        try {
            authorizationService.validateCanManageRoles(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Listing all roles");
        List<Role> roles = roleService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Get a role by code.
     * GET /api/v1/roles/code/{code}
     */
    @GetMapping("/code/{code}")
    public ResponseEntity<Role> getRoleByCode(
            @PathVariable String code,
            @AuthenticationPrincipal UUID callerId) {
        
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // SECURITY: Only admins can view roles
        try {
            authorizationService.validateCanManageRoles(callerId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Getting role by code: {}", code);
        Role role = roleService.getRoleByCode(code);
        return ResponseEntity.ok(role);
    }
}
