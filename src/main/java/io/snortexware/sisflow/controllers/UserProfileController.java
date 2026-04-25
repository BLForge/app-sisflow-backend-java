package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.UpdateProfileRequest;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.services.UserProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
public class UserProfileController {

	private final UserProfileRepository userProfileRepository;
	private final CustomerRepository customerRepository;
	private final UserProfileService userProfileService;
	private final UserRoleRepository userRoleRepository;
	

	public UserProfileController(UserProfileRepository userProfileRepository, CustomerRepository customerRepository, UserProfileService userProfileService, UserRoleRepository userRoleRepository) {
		
		this.userProfileService = userProfileService;
		this.userProfileRepository = userProfileRepository;
		this.customerRepository = customerRepository;
		this.userRoleRepository = userRoleRepository;
	}

	@PostMapping("/sync")
	@Transactional
	public ResponseEntity<UserProfile> sync(@AuthenticationPrincipal UUID callerId,
			@RequestBody(required = false) SyncRequest request) {
		return ResponseEntity.ok(userProfileRepository.findById(callerId).orElseGet(() -> {
			UserProfile profile = UserProfile.builder().id(callerId).name(request != null ? request.name() : null)
					.avatarUrl(request != null ? request.avatarUrl() : null).role(UserProfile.Role.agent)
					.createdAt(OffsetDateTime.now()).build();
			return userProfileRepository.save(profile);
		}));
	}

	@GetMapping("/me")
	public ResponseEntity<UserProfile> me(@AuthenticationPrincipal UUID callerId) {
		UserProfile profile = userProfileRepository.getUserProfileById(callerId);
		if (profile == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok(profile);
	}

	@GetMapping("/me/roles")
	public ResponseEntity<List<Role>> getMyRoles(@AuthenticationPrincipal UUID callerId) {
		if (callerId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		
		try {
			List<UserRole> userRoles = userRoleRepository.findActiveByUserId(callerId);
			List<Role> roles = userRoles.stream()
				.map(UserRole::getRole)
				.toList();
			return ResponseEntity.ok(roles);
		} catch (Exception e) {
			// Se der erro, retorna lista vazia por segurança
			return ResponseEntity.ok(List.of());
		}
	}

	@PutMapping("/me")
	@Transactional
	public ResponseEntity<UserProfile> update(@AuthenticationPrincipal UUID callerId,
			@Valid @RequestBody UpdateProfileRequest request) {
		UserProfile profile = userProfileRepository.getUserProfileById(callerId);
		if (profile == null)
			return ResponseEntity.notFound().build();

		profile.setName(request.getName());
		profile.setAvatarUrl(request.getAvatarUrl());

		return ResponseEntity.ok(userProfileRepository.save(profile));
	}

	@GetMapping("/customer/{customerId}")
	public ResponseEntity<List<UserProfile>> listByCustomer(@PathVariable UUID customerId) {
		return ResponseEntity.ok(userProfileRepository.findByCustomerId(customerId));
	}

	@GetMapping("/customer/{customerId}/users")
	public ResponseEntity<List<UserProfile>> list(@PathVariable UUID costumerID) {
		return ResponseEntity.ok(userProfileRepository.findByCustomer_IdNot(costumerID));
	}

	@PostMapping("/customer/{customerId}")
	@Transactional
	public ResponseEntity<UserProfile> createUserForCustomer(
			@PathVariable UUID customerId,
			@Valid @RequestBody CreateUserProfileRequest request) {
		var userProfile = userProfileService.linkUserToCustomer(customerId, request.userId());
		return ResponseEntity.ok(userProfileRepository.save(userProfile));
	}

	@PutMapping("/{userId}")
	@Transactional
	public ResponseEntity<UserProfile> updateUser(@PathVariable UUID userId,
			@Valid @RequestBody UpdateUserProfileRequest request) {
		UserProfile profile = userProfileRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (request.name() != null)
			profile.setName(request.name());
		if (request.avatarUrl() != null)
			profile.setAvatarUrl(request.avatarUrl());
		if (request.role() != null)
			profile.setRole(request.role());

		if (request.customerId() != null) {
			Customer customer = customerRepository.findById(request.customerId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
			profile.setCustomer(customer);
		}

		return ResponseEntity.ok(userProfileRepository.save(profile));
	}

	@DeleteMapping("/{userId}")
	@Transactional
	public ResponseEntity<Void> deleteUser(@PathVariable UUID userId) {
		if (!userProfileRepository.existsById(userId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		userProfileRepository.deleteById(userId);
		return ResponseEntity.noContent().build();
	}

	public record SyncRequest(String name, String avatarUrl) {
	}

	public record CreateUserProfileRequest(

		    @NotBlank(message = "userId cannot be empty")
		    String userId,

		    @NotNull(message = "role is required")
		    UserProfile.Role role

		) {}

	public record UpdateUserProfileRequest(String name, String avatarUrl, UserProfile.Role role, UUID customerId) {
	}
}
