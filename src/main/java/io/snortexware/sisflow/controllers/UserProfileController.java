package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.UpdateProfileRequest;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.UserProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserProfileService userProfileService;
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;

    @PostMapping("/sync")
    @Transactional
    public ResponseEntity<UserProfile> sync(@AuthenticationPrincipal UUID callerId,
            @RequestBody(required = false) SyncRequest request) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(userProfileRepository.findById(callerId).orElseGet(() -> {
            UserProfile profile = UserProfile.builder()
                    .id(callerId)
                    .name(request != null ? request.name() : null)
                    .avatarUrl(request != null ? request.avatarUrl() : null)
                    .role(UserProfile.Role.agent)
                    .createdAt(OffsetDateTime.now())
                    .build();
            return userProfileRepository.save(profile);
        }));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> me(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return userProfileRepository.findById(callerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me/roles")
    public ResponseEntity<List<Role>> getMyRoles(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<Role> roles = userRoleRepository.findActiveByUserId(callerId).stream()
                .map(UserRole::getRole).toList();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<UserProfile>> listCustomerUsers(@PathVariable UUID customerId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authorizationService.isModeratorOrAbove(callerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        return ResponseEntity.ok(userProfileRepository.findByCustomerId(customerId));
    }

    @PostMapping("/customer/{customerId}")
    @Transactional
    public ResponseEntity<UserProfile> linkUserToCustomer(@PathVariable UUID customerId,
            @Valid @RequestBody LinkUserRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authorizationService.isModeratorOrAbove(callerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        return ResponseEntity.ok(userProfileService.linkUserToCustomer(customerId, request.userId().toString()));
    }

    @PutMapping("/me")
    @Transactional
    public ResponseEntity<UserProfile> update(@AuthenticationPrincipal UUID callerId,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        UserProfile profile = userProfileRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        profile.setName(request.getName());
        profile.setAvatarUrl(request.getAvatarUrl());
        return ResponseEntity.ok(userProfileRepository.save(profile));
    }

    /** Admin/moderator: update any user within the same tenant */
    @PutMapping("/{userId}")
    @Transactional
    public ResponseEntity<UserProfile> updateUser(@PathVariable UUID userId,
            @AuthenticationPrincipal UUID callerId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authorizationService.isModeratorOrAbove(callerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Tenant isolation: can only modify users in same tenant
        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant != null && profile.getTenant() != null
                && !profile.getTenant().getId().equals(callerTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (request.name() != null) profile.setName(request.name());
        if (request.avatarUrl() != null) profile.setAvatarUrl(request.avatarUrl());
        // Only system_admin can change roles
        if (request.role() != null && authorizationService.isAdminOrAbove(callerId))
            profile.setRole(request.role());

        return ResponseEntity.ok(userProfileRepository.save(profile));
    }

    /** Admin only: delete user within same tenant */
    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!authorizationService.isModeratorOrAbove(callerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Tenant isolation
        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant != null && profile.getTenant() != null
                && !profile.getTenant().getId().equals(callerTenant)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        // Prevent self-deletion
        if (userId.equals(callerId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");

        userProfileRepository.deleteById(userId);
        return ResponseEntity.noContent().build();
    }

    public record SyncRequest(String name, String avatarUrl) {}
    public record LinkUserRequest(@NotNull UUID userId) {}
    public record CreateUserProfileRequest(
            @NotBlank String userId,
            @NotNull UserProfile.Role role) {}
    public record UpdateUserProfileRequest(String name, String avatarUrl, UserProfile.Role role) {}
}
