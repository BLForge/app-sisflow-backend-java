package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
public class UserRoleController {

    private final RoleService roleService;
    private final AuthorizationService authorizationService;
    private final RoleRepository roleRepository;
    private final UserProfileRepository userProfileRepository;
    private final TenantContext tenantContext;

    @PostMapping("/roles/{roleId}")
    @Transactional
    public ResponseEntity<Void> assignRoleToUser(@PathVariable UUID userId, @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        assertSameTenant(callerId, userId);

        Role role = roleRepository.findById(roleId).orElseThrow(AppException::notFound);
        authorizationService.validateCanAssignRole(callerId, role.getHierarchyLevel());

        roleService.assignRoleToUser(userId, roleId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/roles/{roleId}")
    @Transactional
    public ResponseEntity<Void> removeRoleFromUser(@PathVariable UUID userId, @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        assertSameTenant(callerId, userId);
        roleService.removeRoleFromUser(userId, roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getUserRoles(@PathVariable UUID userId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        assertSameTenant(callerId, userId);
        return ResponseEntity.ok(roleService.getUserRoles(userId));
    }

    private void assertSameTenant(UUID callerId, UUID targetUserId) {
        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant == null) return;
        UserProfile target = userProfileRepository.findById(targetUserId).orElseThrow(AppException::notFound);
        if (target.getTenant() == null || !target.getTenant().getId().equals(callerTenant))
            throw AppException.forbidden();
    }
}
