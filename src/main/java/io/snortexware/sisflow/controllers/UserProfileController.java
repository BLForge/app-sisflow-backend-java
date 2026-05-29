package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.UpdateProfileRequest;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.UserProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final AuthorizationService authorizationService;

    @PostMapping("/sync")
    @Transactional
    public ResponseEntity<UserProfile> sync(@AuthenticationPrincipal UUID callerId,
            @RequestBody(required = false) SyncRequest request) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(userProfileService.sync(
                callerId,
                request != null ? request.name() : null,
                request != null ? request.avatarUrl() : null));
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfile> me(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(userProfileService.getMe(callerId));
    }

    @GetMapping("/me/roles")
    public ResponseEntity<List<Role>> getMyRoles(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(userProfileService.getMyRoles(callerId));
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<UserProfile>> listCustomerUsers(@PathVariable UUID customerId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(userProfileService.listCustomerUsers(customerId));
    }

    @PostMapping("/customer/{customerId}")
    @Transactional
    public ResponseEntity<UserProfile> linkUserToCustomer(@PathVariable UUID customerId,
            @Valid @RequestBody LinkUserRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(userProfileService.linkUserToCustomer(customerId, request.userId().toString()));
    }

    @PutMapping("/me")
    @Transactional
    public ResponseEntity<UserProfile> update(@AuthenticationPrincipal UUID callerId,
            @Valid @RequestBody UpdateProfileRequest request) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(userProfileService.updateMe(callerId, request));
    }

    @PutMapping("/{userId}")
    @Transactional
    public ResponseEntity<UserProfile> updateUser(@PathVariable UUID userId,
            @AuthenticationPrincipal UUID callerId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        return ResponseEntity.ok(userProfileService.updateUser(
                userId, callerId, request.name(), request.avatarUrl(), request.role()));
    }

    @DeleteMapping("/{userId}")
    @Transactional
    public ResponseEntity<Void> deleteUser(@PathVariable UUID userId, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        if (!authorizationService.isModeratorOrAbove(callerId)) throw AppException.forbidden();
        userProfileService.deleteUser(userId, callerId);
        return ResponseEntity.noContent().build();
    }

    public record SyncRequest(String name, String avatarUrl) {}
    public record LinkUserRequest(@NotNull UUID userId) {}
    public record UpdateUserProfileRequest(String name, String avatarUrl, UserProfile.Role role) {}
}
