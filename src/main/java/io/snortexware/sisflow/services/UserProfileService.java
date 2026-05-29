package io.snortexware.sisflow.services;

import java.util.UUID;

import io.snortexware.sisflow.security.exceptions.AppException;
import org.springframework.stereotype.Service;

import io.snortexware.sisflow.entities.Customer;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.CustomerRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;

@Service
public class UserProfileService {

	private final UserProfileRepository userProfileRepository;
	private final CustomerRepository customerRepository;

	public UserProfileService(UserProfileRepository userProfileRepository, CustomerRepository customerRepository) {
		this.userProfileRepository = userProfileRepository;
		this.customerRepository = customerRepository;
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

	private UUID parseUUID(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			throw AppException.badRequest();
		}
	}
}
