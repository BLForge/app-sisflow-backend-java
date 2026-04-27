package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers(@AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);

        UUID tenantId = tenantContext.getCurrentTenant();
        List<UserProfile> users = tenantId != null
                ? userProfileRepository.findByTenant_Id(tenantId)
                : userProfileRepository.findAll();

        List<Map<String, Object>> result = users.stream().map(user -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", user.getId());
            m.put("name", user.getName());
            m.put("email", user.getEmail());
            m.put("avatarUrl", user.getAvatarUrl());
            m.put("createdAt", user.getCreatedAt());
            List<Map<String, Object>> roles = userRoleRepository.findActiveByUserId(user.getId()).stream()
                    .map(ur -> Map.<String, Object>of(
                            "id", ur.getRole().getId(),
                            "code", ur.getRole().getCode(),
                            "name", ur.getRole().getName(),
                            "hierarchyLevel", ur.getRole().getHierarchyLevel()))
                    .collect(Collectors.toList());
            m.put("roles", roles);
            m.put("highestRoleLevel", roles.stream().mapToInt(r -> (Integer) r.get("hierarchyLevel")).max().orElse(-1));
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getRoles(@AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        // Tenant admins can only see roles up to their own level
        int callerLevel = authorizationService.getCurrentUserHierarchyLevel(callerId);
        List<Role> roles = roleRepository.findAll().stream()
                .filter(r -> r.getHierarchyLevel() <= callerLevel)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roles);
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @Transactional
    public ResponseEntity<Void> assignRole(@PathVariable UUID userId, @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        assertSameTenant(callerId, userId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));

        // Prevent privilege escalation: can't assign a role higher than your own
        authorizationService.validateCanAssignRole(callerId, role.getHierarchyLevel());

        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        userRoleRepository.save(UserRole.builder()
                .user(user).role(role).isActive(true)
                .assignedAt(OffsetDateTime.now()).assignedBy(callerId)
                .createdAt(OffsetDateTime.now()).build());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @Transactional
    public ResponseEntity<Void> removeRole(@PathVariable UUID userId, @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        assertSameTenant(callerId, userId);

        UserRole userRole = userRoleRepository.findActiveByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User role not found"));

        userRole.setIsActive(false);
        userRole.setRevokedAt(OffsetDateTime.now());
        userRole.setRevokedBy(callerId);
        userRoleRepository.save(userRole);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> updateUser(@PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        assertSameTenant(callerId, userId);

        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.name() != null) user.setName(request.name());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        userProfileRepository.save(user);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId,
            @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        if (userId.equals(callerId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
        assertSameTenant(callerId, userId);
        userProfileRepository.deleteById(userId);
        return ResponseEntity.noContent().build();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void requireModerator(UUID callerId) {
        if (callerId == null || !authorizationService.isModeratorOrAbove(callerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
    }

    private void assertSameTenant(UUID callerId, UUID targetUserId) {
        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant == null) return; // system_admin has no tenant restriction
        userProfileRepository.findById(targetUserId).ifPresent(target -> {
            if (target.getTenant() != null && !target.getTenant().getId().equals(callerTenant))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        });
    }

    public record UpdateUserRequest(String name, String avatarUrl) {}
}
