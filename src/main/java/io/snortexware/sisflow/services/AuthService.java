package io.snortexware.sisflow.services;

import io.snortexware.sisflow.entities.EmailConfirmationToken;
import io.snortexware.sisflow.entities.PasswordResetToken;
import io.snortexware.sisflow.entities.RefreshToken;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.EmailConfirmationTokenRepository;
import io.snortexware.sisflow.repositories.PasswordResetTokenRepository;
import io.snortexware.sisflow.repositories.RefreshTokenRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserProfileRepository userProfileRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final JwtService jwtService;
	private final BCryptPasswordEncoder passwordEncoder;
	private final EmailService emailService;
	private final TenantResolver tenantResolver;
	private final LoginAttemptService loginAttemptService;

	@Value("${jwt.refresh-expiration-days:1}")
	private long refreshExpirationDays;

	@Value("${email.confirmation.expiration-hours:24}")
	private long confirmationExpirationHours;

	@Transactional
	public Map<String, Object> signIn(String email, String password, HttpServletRequest request) {
		String ipAddress = getClientIp(request);
		loginAttemptService.validateLoginAttempt(email, ipAddress);

		UserProfile user = userProfileRepository.findByEmailWithTenant(email).orElseThrow(() -> {
			loginAttemptService.recordLoginAttempt(email, ipAddress, false);
			return AppException.notFound();
		});

		var tenant = tenantResolver.resolveFromRequest(request);
		if (tenant.isPresent()) {
			if (user.getTenant() == null || !user.getTenant().getId().equals(tenant.get().getId())) {
				loginAttemptService.recordLoginAttempt(email, ipAddress, false);
				throw AppException.notFound();
			}
		} else {
			if (user.getTenant() != null || !userProfileRepository.hasSystemAdminRole(user.getId())) {
				loginAttemptService.recordLoginAttempt(email, ipAddress, false);
				throw AppException.notFound();
			}
		}

		if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
			loginAttemptService.recordLoginAttempt(email, ipAddress, false);
			throw AppException.badRequest();
		}

		if (!user.isEmailConfirmed()) {
			loginAttemptService.recordLoginAttempt(email, ipAddress, false);
			throw AppException.forbidden();
		}

		loginAttemptService.recordLoginAttempt(email, ipAddress, true);
		UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
		return buildTokenResponse(user.getId(), tenantId);
	}

	@Transactional
	public Map<String, Object> signUp(String email, String password, String fullName) {
		if (userProfileRepository.findByEmail(email).isPresent()) {
			return Map.of("status", "pending_confirmation");
		}

		UserProfile user = UserProfile.builder().id(UUID.randomUUID()).email(email)
				.passwordHash(passwordEncoder.encode(password)).name(fullName).role(UserProfile.Role.client)
				.emailConfirmed(false).createdAt(OffsetDateTime.now()).build();

		userProfileRepository.save(user);
		emailService.sendConfirmationEmail(email, issueConfirmationToken(user.getId()));
		return Map.of("status", "pending_confirmation");
	}

	@Transactional
	public Map<String, Object> confirmEmail(String token) {
		EmailConfirmationToken ct = emailConfirmationTokenRepository.findByToken(token)
				.orElseThrow(AppException::badRequest);

		if (ct.isUsed() || ct.getExpiresAt().isBefore(OffsetDateTime.now()))
			throw AppException.badRequest();

		UserProfile user = userProfileRepository.findById(ct.getUserId()).orElseThrow(AppException::notFound);

		user.setEmailConfirmed(true);
		userProfileRepository.save(user);
		ct.setUsed(true);
		emailConfirmationTokenRepository.save(ct);

		UUID tenantId = user.getTenant() != null ? user.getTenant().getId() : null;
		return buildTokenResponse(user.getId(), tenantId);
	}

	@Transactional
	public void resendConfirmation(String email) {
		userProfileRepository.findByEmail(email).ifPresent(user -> {
			if (!user.isEmailConfirmed()) {
				emailConfirmationTokenRepository.deleteByUserId(user.getId());
				emailService.sendConfirmationEmail(email, issueConfirmationToken(user.getId()));
			}
		});
	}

	@Transactional
	public void requestPasswordReset(String email) {
		userProfileRepository.findByEmail(email).ifPresent(user -> {
			passwordResetTokenRepository.deleteByUserId(user.getId());
			String token = UUID.randomUUID().toString();
			passwordResetTokenRepository.save(PasswordResetToken.builder().userId(user.getId()).token(token)
					.expiresAt(OffsetDateTime.now().plusHours(1)).used(false).createdAt(OffsetDateTime.now()).build());
			emailService.sendPasswordResetEmail(email, token);
		});
	}

	@Transactional
	public void resetPassword(String token, String newPassword) {
		PasswordResetToken prt = passwordResetTokenRepository.findByToken(token).orElseThrow(AppException::badRequest);

		if (prt.isUsed() || prt.getExpiresAt().isBefore(OffsetDateTime.now()))
			throw AppException.badRequest();

		UserProfile user = userProfileRepository.findById(prt.getUserId()).orElseThrow(AppException::notFound);

		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userProfileRepository.save(user);
		prt.setUsed(true);
		passwordResetTokenRepository.save(prt);
	}

	@Transactional
	public Map<String, Object> refresh(String rawRefreshToken) {
		RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
				.orElseThrow(AppException::unauthorized);

		if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
			refreshTokenRepository.delete(stored);
			throw AppException.unauthorized();
		}

		refreshTokenRepository.delete(stored);
		UUID tenantId = userProfileRepository.findByIdWithTenant(stored.getUserId())
				.map(u -> u.getTenant() != null ? u.getTenant().getId() : null).orElse(null);
		return buildTokenResponse(stored.getUserId(), tenantId);
	}

	private String issueConfirmationToken(UUID userId) {
		String token = UUID.randomUUID().toString();
		emailConfirmationTokenRepository.save(EmailConfirmationToken.builder().userId(userId).token(token)
				.expiresAt(OffsetDateTime.now().plusHours(confirmationExpirationHours)).used(false)
				.createdAt(OffsetDateTime.now()).build());
		return token;
	}

	private Map<String, Object> buildTokenResponse(UUID userId, UUID tenantId) {
		String accessToken = jwtService.generateToken(userId, tenantId);
		String refreshToken = UUID.randomUUID().toString();
		refreshTokenRepository.save(RefreshToken.builder().userId(userId).token(refreshToken)
				.expiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays)).createdAt(OffsetDateTime.now())
				.build());
		return Map.of("accessToken", accessToken, "refreshToken", refreshToken, "expiresIn", 3600);
	}

	private String getClientIp(HttpServletRequest request) {
		String xForwardedFor = request.getHeader("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			return xForwardedFor.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
