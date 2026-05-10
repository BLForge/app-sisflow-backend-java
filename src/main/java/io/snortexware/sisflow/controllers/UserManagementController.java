package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthService;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final AuthorizationService authorizationService;
    private final AuthService authService;
    private final TenantContext tenantContext;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        if (userProfileRepository.findByEmail(request.email()).isPresent()) throw AppException.conflict();

        UUID tenantId = tenantContext.getCurrentTenant();
        Tenant tenant = tenantId != null
                ? tenantRepository.findById(tenantId).orElseThrow(AppException::notFound)
                : null;

        UserProfile user = UserProfile.builder()
                .id(UUID.randomUUID()).email(request.email()).name(request.name())
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .tenant(tenant).type(UserProfile.UserType.agent).role(UserProfile.Role.agent)
                .emailConfirmed(true).createdAt(OffsetDateTime.now()).build();

        userProfileRepository.save(user);
        authService.requestPasswordReset(user.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", user.getId(), "email", user.getEmail(), "name", user.getName()));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listUsers(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();

        UUID tenantId = tenantContext.getCurrentTenant();
        List<UserProfile> users = tenantId != null
                ? userProfileRepository.findByTenant_Id(tenantId)
                : userProfileRepository.findAll();

        List<Map<String, Object>> result = users.stream().map(user -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", user.getId()); m.put("name", user.getName());
            m.put("email", user.getEmail()); m.put("avatarUrl", user.getAvatarUrl());
            m.put("createdAt", user.getCreatedAt());
            List<Map<String, Object>> roles = userRoleRepository.findActiveByUserId(user.getId()).stream()
                    .map(ur -> Map.<String, Object>of(
                            "id", ur.getRole().getId(), "code", ur.getRole().getCode(),
                            "name", ur.getRole().getName(), "hierarchyLevel", ur.getRole().getHierarchyLevel()))
                    .collect(Collectors.toList());
            m.put("roles", roles);
            m.put("highestRoleLevel", roles.stream().mapToInt(r -> (Integer) r.get("hierarchyLevel")).max().orElse(-1));
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> getRoles(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        int callerLevel = authorizationService.getCurrentUserHierarchyLevel(callerId);
        return ResponseEntity.ok(roleRepository.findAll().stream()
                .filter(r -> r.getHierarchyLevel() <= callerLevel).collect(Collectors.toList()));
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @Transactional
    public ResponseEntity<Void> assignRole(@PathVariable UUID userId, @PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        assertSameTenant(callerId, userId);

        Role role = roleRepository.findById(roleId).orElseThrow(AppException::notFound);
        authorizationService.validateCanAssignRole(callerId, role.getHierarchyLevel());

        UserProfile user = userProfileRepository.findById(userId).orElseThrow(AppException::notFound);
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
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        assertSameTenant(callerId, userId);

        UserRole userRole = userRoleRepository.findActiveByUserIdAndRoleId(userId, roleId)
                .orElseThrow(AppException::notFound);
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
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        assertSameTenant(callerId, userId);

        UserProfile user = userProfileRepository.findById(userId).orElseThrow(AppException::notFound);
        if (request.name() != null) user.setName(request.name());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        userProfileRepository.save(user);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        if (userId.equals(callerId)) throw AppException.badRequest();
        assertSameTenant(callerId, userId);
        userProfileRepository.deleteById(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID userId, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        assertSameTenant(callerId, userId);
        UserProfile user = userProfileRepository.findById(userId).orElseThrow(AppException::notFound);
        authService.requestPasswordReset(user.getEmail());
        return ResponseEntity.noContent().build();
    }

    private void assertSameTenant(UUID callerId, UUID targetUserId) {
        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant == null) return;
        userProfileRepository.findById(targetUserId).ifPresent(target -> {
            if (target.getTenant() != null && !target.getTenant().getId().equals(callerTenant))
                throw AppException.forbidden();
        });
    }

    public record UpdateUserRequest(String name, String avatarUrl) {}
    public record CreateUserRequest(@NotBlank @Email String email, @NotBlank String name) {}
}
