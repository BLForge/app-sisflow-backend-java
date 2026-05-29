package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.UpdateProfileRequest;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserProfileService {

	private final UserProfileRepository userProfileRepository;
	private final CustomerRepository customerRepository;
    private final UserRoleRepository userRoleRepository;
    private final TenantContext tenantContext;
    private final AuthorizationService authorizationService;

	public UserProfileService(
            UserProfileRepository userProfileRepository,
            CustomerRepository customerRepository,
            UserRoleRepository userRoleRepository,
            TenantContext tenantContext,
            AuthorizationService authorizationService) {
		this.userProfileRepository = userProfileRepository;
		this.customerRepository = customerRepository;
        this.userRoleRepository = userRoleRepository;
        this.tenantContext = tenantContext;
        this.authorizationService = authorizationService;
	}

    @Transactional
    public UserProfile sync(UUID callerId, String name, String avatarUrl) {
        return userProfileRepository.findById(callerId).orElseGet(() -> {
            UserProfile profile = UserProfile.builder()
                    .id(callerId)
                    .name(name)
                    .avatarUrl(avatarUrl)
                    .role(UserProfile.Role.agent)
                    .createdAt(OffsetDateTime.now())
                    .build();
            return userProfileRepository.save(profile);
        });
    }

    public UserProfile getMe(UUID callerId) {
        return userProfileRepository.findById(callerId).orElseThrow(AppException::notFound);
    }

    public List<Role> getMyRoles(UUID callerId) {
        return userRoleRepository.findActiveByUserId(callerId).stream().map(UserRole::getRole).toList();
    }

    public List<UserProfile> listCustomerUsers(UUID customerId) {
        return userProfileRepository.findByCustomerId(customerId);
    }

	public UserProfile linkUserToCustomer(UUID customerId, String userIdRaw) {
		UUID userId = parseUUID(userIdRaw);

		Customer customer = customerRepository.findById(customerId).orElseThrow(AppException::notFound);
		UserProfile userProfile = userProfileRepository.findById(userId).orElseThrow(AppException::notFound);

		if (userProfile.getCustomer() != null)
			throw AppException.conflict();

		userProfile.setCustomer(customer);
		return userProfileRepository.save(userProfile);
	}

    @Transactional
    public UserProfile updateMe(UUID callerId, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(callerId).orElseThrow(AppException::notFound);
        profile.setName(request.getName());
        profile.setAvatarUrl(request.getAvatarUrl());
        return userProfileRepository.save(profile);
    }

    @Transactional
    public UserProfile updateUser(UUID userId, UUID callerId, String name, String avatarUrl, UserProfile.Role role) {
        UserProfile profile = userProfileRepository.findById(userId).orElseThrow(AppException::notFound);

        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant != null && profile.getTenant() != null
                && !profile.getTenant().getId().equals(callerTenant))
            throw AppException.forbidden();

        if (name != null) profile.setName(name);
        if (avatarUrl != null) profile.setAvatarUrl(avatarUrl);
        if (role != null && authorizationService.isAdminOrAbove(callerId))
            profile.setRole(role);

        return userProfileRepository.save(profile);
    }

    @Transactional
    public void deleteUser(UUID userId, UUID callerId) {
        UserProfile profile = userProfileRepository.findById(userId).orElseThrow(AppException::notFound);

        UUID callerTenant = tenantContext.getCurrentTenant();
        if (callerTenant != null && profile.getTenant() != null
                && !profile.getTenant().getId().equals(callerTenant))
            throw AppException.forbidden();

        if (userId.equals(callerId)) throw AppException.badRequest();

        userProfileRepository.deleteById(userId);
    }

	private UUID parseUUID(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			throw AppException.badRequest();
		}
	}
}
