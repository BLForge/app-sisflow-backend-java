package io.snortexware.sisflow.services;

import io.snortexware.sisflow.entities.EmailConfirmationToken;
import io.snortexware.sisflow.entities.PasswordResetToken;
import io.snortexware.sisflow.entities.RefreshToken;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.EmailConfirmationTokenRepository;
import io.snortexware.sisflow.repositories.PasswordResetTokenRepository;
import io.snortexware.sisflow.repositories.RefreshTokenRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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

    @Value("${jwt.refresh-expiration-days:30}")
    private long refreshExpirationDays;

    @Value("${email.confirmation.expiration-hours:24}")
    private long confirmationExpirationHours;

    @Transactional
    public Map<String, Object> signIn(String email, String password) {
        UserProfile user = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credenciais inválidas"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credenciais inválidas");

        if (!user.isEmailConfirmed())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "E-mail não confirmado. Verifique sua caixa de entrada.");

        return buildTokenResponse(user.getId());
    }

    @Transactional
    public Map<String, Object> signUp(String email, String password, String fullName) {
        if (userProfileRepository.findByEmail(email).isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já cadastrado");

        UserProfile user = UserProfile.builder()
                .id(UUID.randomUUID())
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .name(fullName)
                .role(UserProfile.Role.client)
                .emailConfirmed(false)
                .createdAt(OffsetDateTime.now())
                .build();

        userProfileRepository.save(user);
        emailService.sendConfirmationEmail(email, issueConfirmationToken(user.getId()));
        return Map.of("status", "pending_confirmation");
    }

    @Transactional
    public Map<String, Object> confirmEmail(String token) {
        EmailConfirmationToken ct = emailConfirmationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado"));

        if (ct.isUsed() || ct.getExpiresAt().isBefore(OffsetDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado");

        UserProfile user = userProfileRepository.findById(ct.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setEmailConfirmed(true);
        userProfileRepository.save(user);
        ct.setUsed(true);
        emailConfirmationTokenRepository.save(ct);

        return buildTokenResponse(user.getId());
    }

    @Transactional
    public void resendConfirmation(String email) {
        UserProfile user = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (user.isEmailConfirmed())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail já confirmado");

        emailConfirmationTokenRepository.deleteByUserId(user.getId());
        emailService.sendConfirmationEmail(email, issueConfirmationToken(user.getId()));
    }

    @Transactional
    public void requestPasswordReset(String email) {
        userProfileRepository.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepository.deleteByUserId(user.getId());
            String token = UUID.randomUUID().toString();
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .expiresAt(OffsetDateTime.now().plusHours(1))
                    .used(false)
                    .createdAt(OffsetDateTime.now())
                    .build());
            emailService.sendPasswordResetEmail(email, token);
        });
        // Always return silently to avoid user enumeration
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado"));

        if (prt.isUsed() || prt.getExpiresAt().isBefore(OffsetDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token inválido ou expirado");

        UserProfile user = userProfileRepository.findById(prt.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userProfileRepository.save(user);
        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);
    }

    @Transactional
    public Map<String, Object> refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(rawRefreshToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido"));

        if (stored.getExpiresAt().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado");
        }

        refreshTokenRepository.delete(stored);
        return buildTokenResponse(stored.getUserId());
    }

    private String issueConfirmationToken(UUID userId) {
        String token = UUID.randomUUID().toString();
        emailConfirmationTokenRepository.save(EmailConfirmationToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(OffsetDateTime.now().plusHours(confirmationExpirationHours))
                .used(false)
                .createdAt(OffsetDateTime.now())
                .build());
        return token;
    }

    private Map<String, Object> buildTokenResponse(UUID userId) {
        String accessToken = jwtService.generateToken(userId);
        String refreshToken = UUID.randomUUID().toString();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .token(refreshToken)
                .expiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays))
                .createdAt(OffsetDateTime.now())
                .build());
        return Map.of("accessToken", accessToken, "refreshToken", refreshToken, "expiresIn", 3600);
    }
}
