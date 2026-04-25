package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User Management Controller - Admin only Allows system administrators to
 * manage users and their roles
 */
@Slf4j
@RestController
@RequestMapping("/admin/users")
public class UserManagementController {

	private final UserProfileRepository userProfileRepository;
	private final UserRoleRepository userRoleRepository;
	private final RoleRepository roleRepository;
	private final AuthorizationService authorizationService;

	public UserManagementController(UserProfileRepository userProfileRepository, UserRoleRepository userRoleRepository,
			RoleRepository roleRepository, AuthorizationService authorizationService) {
		this.userProfileRepository = userProfileRepository;
		this.userRoleRepository = userRoleRepository;
		this.roleRepository = roleRepository;
		this.authorizationService = authorizationService;
	}

	/**
	 * List all users with their roles (Admin only)
	 */
	@GetMapping
	public ResponseEntity<List<Map<String, Object>>> listUsers(@AuthenticationPrincipal UUID callerId) {
		if (!isAuthorized(callerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		try {
			List<UserProfile> users = userProfileRepository.findAll();
			List<Map<String, Object>> userList = users.stream().map(user -> {
				Map<String, Object> userMap = new HashMap<>();
				userMap.put("id", user.getId());
				userMap.put("name", user.getName());
				userMap.put("avatarUrl", user.getAvatarUrl());
				userMap.put("createdAt", user.getCreatedAt());

				// Get user roles
				List<UserRole> userRoles = userRoleRepository.findActiveByUserId(user.getId());
				List<Map<String, Object>> roles = userRoles.stream().map(ur -> {
					Map<String, Object> roleMap = new HashMap<>();
					roleMap.put("id", ur.getRole().getId());
					roleMap.put("code", ur.getRole().getCode());
					roleMap.put("name", ur.getRole().getName());
					roleMap.put("hierarchyLevel", ur.getRole().getHierarchyLevel());
					return roleMap;
				}).collect(Collectors.toList());

				userMap.put("roles", roles);
				userMap.put("highestRoleLevel",
						roles.stream().mapToInt(r -> (Integer) r.get("hierarchyLevel")).max().orElse(-1));

				return userMap;
			}).collect(Collectors.toList());

			return ResponseEntity.ok(userList);
		} catch (Exception e) {
			log.error("Error listing users", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Get all available roles (Admin only)
	 */
	@GetMapping("/roles")
	public ResponseEntity<List<Role>> getRoles(@AuthenticationPrincipal UUID callerId) {
		if (!isAuthorized(callerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		try {
			List<Role> roles = roleRepository.findAll();
			return ResponseEntity.ok(roles);
		} catch (Exception e) {
			log.error("Error getting roles", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Create a new user (Admin only)
	 */
	@PostMapping
	@Transactional
	public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody CreateUserRequest request,
			@AuthenticationPrincipal UUID callerId) {
		if (!isAuthorized(callerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Map<String, Object> response = new HashMap<>();

		try {
			// Check if user already exists
			if (userProfileRepository.existsById(UUID.fromString(request.userId))) {
				response.put("error", "User already exists");
				return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
			}

			// Create user profile
			UserProfile userProfile = UserProfile.builder().id(UUID.fromString(request.userId)).name(request.name)
					.avatarUrl(request.avatarUrl).role(UserProfile.Role.agent) // Default role
					.createdAt(OffsetDateTime.now()).build();

			userProfile = userProfileRepository.save(userProfile);

			// Assign role if specified
			if (request.roleId != null) {
				Role role = roleRepository.findById(UUID.fromString(request.roleId))
						.orElseThrow(() -> new RuntimeException("Role not found"));

				UserRole userRole = UserRole.builder().id(UUID.randomUUID()).user(userProfile).role(role).isActive(true)
						.assignedAt(OffsetDateTime.now()).assignedBy(callerId).createdAt(OffsetDateTime.now()).build();

				userRoleRepository.save(userRole);
			}

			response.put("success", true);
			response.put("message", "User created successfully");
			response.put("userId", userProfile.getId());

			log.info("Admin {} created user {}", callerId, userProfile.getId());
			return ResponseEntity.status(HttpStatus.CREATED).body(response);

		} catch (Exception e) {
			log.error("Error creating user", e);
			response.put("error", "Failed to create user");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	/**
	 * Assign role to user (Admin only)
	 */
	@PostMapping("/{userId}/roles/{roleId}")
	public ResponseEntity<Map<String, Object>> assignRole(@PathVariable String userId,
			@PathVariable String roleId,
			@AuthenticationPrincipal UUID callerId) {

		if (!isAuthorized(callerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Map<String, Object> response = new HashMap<>();

		UUID userUuid;
		UUID roleUuid;

		try {
			userUuid = UUID.fromString(userId);
			roleUuid = UUID.fromString(roleId);
		} catch (IllegalArgumentException e) {
			response.put("error", "Invalid UUID format");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		UserProfile user = userProfileRepository.findById(userUuid).orElse(null);

		if (user == null) {
			response.put("error", "User not found");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		Role role = roleRepository.findById(roleUuid).orElse(null);

		if (role == null) {
			response.put("error", "Role not found");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}

		OffsetDateTime now = OffsetDateTime.now();

		UserRole userRole = UserRole.builder().id(UUID.randomUUID()).user(user).role(role).isActive(true)
				.assignedAt(now).assignedBy(callerId).createdAt(now).build();

		userRoleRepository.save(userRole);

		response.put("success", true);
		response.put("message", "Role assigned successfully");
		return ResponseEntity.ok(response);
	}

	/**
	 * Remove role from user (Admin only)
	 */
	@DeleteMapping("/{userId}/roles/{roleId}")
	@Transactional
	public ResponseEntity<Map<String, Object>> removeRole(@PathVariable String userId, @PathVariable String roleId,
			@AuthenticationPrincipal UUID callerId) {
		if (!isAuthorized(callerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Map<String, Object> response = new HashMap<>();

		try {
			UUID userUuid = UUID.fromString(userId);
			UUID roleUuid = UUID.fromString(roleId);

			// Find and deactivate the user role
			List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userUuid);
			
			UserRole userRole = userRoleRepository
				    .findActiveByUserIdAndRoleId(userUuid, roleUuid)
				    .orElseThrow(() -> new RuntimeException("User role not found"));
			
			userRole.setIsActive(false);
			userRole.setRevokedAt(OffsetDateTime.now());
			userRole.setRevokedBy(callerId);

			userRoleRepository.save(userRole);

			response.put("success", true);
			response.put("message", "Role removed successfully");

			log.info("Admin {} removed role {} from user {}", callerId, roleId, userId);
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("Error removing role", e);
			response.put("error", "Failed to remove role");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	/**
	 * Update user profile (Admin only)
	 */
	@PutMapping("/{userId}")
	@Transactional
	public ResponseEntity<Map<String, Object>> updateUser(@PathVariable String userId,
			@Valid @RequestBody UpdateUserRequest request, @AuthenticationPrincipal UUID callerId) {
		if (!isAuthorized(callerId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Map<String, Object> response = new HashMap<>();

		try {
			UUID userUuid = UUID.fromString(userId);
			UserProfile user = userProfileRepository.findById(userUuid)
					.orElseThrow(() -> new RuntimeException("User not found"));

			if (request.name != null) {
				user.setName(request.name);
			}
			if (request.avatarUrl != null) {
				user.setAvatarUrl(request.avatarUrl);
			}

			userProfileRepository.save(user);

			response.put("success", true);
			response.put("message", "User updated successfully");

			log.info("Admin {} updated user {}", callerId, userId);
			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("Error updating user", e);
			response.put("error", "Failed to update user");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	private boolean isAuthorized(UUID callerId) {
		if (callerId == null) {
			return false;
		}

		try {
			return authorizationService.isAdminOrAbove(callerId);
		} catch (Exception e) {
			log.error("Authorization check failed for user: {}", callerId, e);
			return false;
		}
	}

	// DTOs
	public record CreateUserRequest(@NotBlank String userId, @NotBlank String name, String avatarUrl, String roleId) {
	}

	public record UpdateUserRequest(String name, String avatarUrl) {
	}
}