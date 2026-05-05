package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
public class UserRoleController {

    private final RoleService roleService;
    private final AuthorizationService authorizationService;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final TenantContext tenantContext;

    /** Verify target user is in the same tenant as the caller. */
    private void assertSameTenant(UUID callerId, UUID targetUserId) {
        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant == null) return; // system_admin
        UserProfile target = userProfileRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (target.getTenant() == null || !target.getTenant().getId().equals(callerTenant))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @PostMapping("/roles/{roleId}")
    @Transactional
    public ResponseEntity<Void> assignRoleToUser(
            @PathVariable UUID userId,
            @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {

        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authorizationService.isModeratorOrAbove(callerId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        assertSameTenant(callerId, userId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        authorizationService.validateCanAssignRole(callerId, role.getHierarchyLevel());

        roleService.assignRoleToUser(userId, roleId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/roles/{roleId}")
    @Transactional
    public ResponseEntity<Void> removeRoleFromUser(
            @PathVariable UUID userId,
            @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {

        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authorizationService.isModeratorOrAbove(callerId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        assertSameTenant(callerId, userId);

        roleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getUserRoles(
            @PathVariable UUID userId,
            @AuthenticationPrincipal UUID callerId) {

        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authorizationService.isModeratorOrAbove(callerId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        assertSameTenant(callerId, userId);

        return ResponseEntity.ok(roleService.getUserRoles(userId));
    }
}
