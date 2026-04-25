package io.snortexware.sisflow.services;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

		Customer customer = customerRepository.findById(customerId).orElseThrow(() -> notFound("Customer not found"));

		UserProfile userProfile = userProfileRepository.findById(userId)
				.orElseThrow(() -> notFound("User profile not found"));

		if (userProfile.getCustomer() != null) {
			throw conflict("User already linked to a customer");
		}

		userProfile.setCustomer(customer);

		return userProfileRepository.save(userProfile);
	}

	private UUID parseUUID(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			throw badRequest("Invalid user id format");
		}
	}

	private ResponseStatusException notFound(String msg) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
	}

	private ResponseStatusException badRequest(String msg) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
	}

	private ResponseStatusException conflict(String msg) {
		return new ResponseStatusException(HttpStatus.CONFLICT, msg);
	}
}
