package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.BulkRoleAssignmentRequest;
import io.snortexware.sisflow.entities.Permission;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.security.annotations.RequirePermission;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.PermissionService;
import io.snortexware.sisflow.services.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
public class UserRoleController {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final AuthorizationService authorizationService;
    private final RoleRepository roleRepository;

    @PostMapping("/roles/{roleId}")
    @Transactional
    @RequirePermission("user:update")
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable UUID userId,
            @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        // Prevent privilege escalation: caller cannot assign a role above their own level
        authorizationService.validateCanAssignRole(callerId, role.getHierarchyLevel());

        log.info("Assigning role {} to user {}", roleId, userId);
        roleService.assignRoleToUser(userId, roleId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

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

    @GetMapping("/roles")
    @RequirePermission("user:read")
    public ResponseEntity<List<Role>> getUserRoles(@PathVariable UUID userId) {
        return ResponseEntity.ok(roleService.getUserRoles(userId));
    }

    @GetMapping("/permissions")
    @RequirePermission("user:read")
    public ResponseEntity<Set<Permission>> getUserPermissions(@PathVariable UUID userId) {
        return ResponseEntity.ok(permissionService.getUserPermissions(userId));
    }

    @PostMapping("/roles/bulk-assign")
    @Transactional
    @RequirePermission("user:update")
    public ResponseEntity<Void> bulkAssignRoles(
            @Valid @RequestBody BulkRoleAssignmentRequest request,
            @AuthenticationPrincipal UUID callerId) {

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        // Prevent privilege escalation for bulk assign too
        authorizationService.validateCanAssignRole(callerId, role.getHierarchyLevel());

        log.info("Bulk assigning role {} to {} users", request.getRoleId(), request.getUserIds().size());
        roleService.bulkAssignRoles(request.getUserIds(), request.getRoleId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
