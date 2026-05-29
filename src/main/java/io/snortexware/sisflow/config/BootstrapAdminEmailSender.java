package io.snortexware.sisflow.config;

import io.snortexware.sisflow.repositories.EmailConfirmationTokenRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.services.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.snortexware.sisflow.entities.EmailConfirmationToken;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdminEmailSender implements ApplicationRunner {

    private final UserProfileRepository userProfileRepository;
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final EmailService emailService;

    @Value("${BOOTSTRAP_ADMIN_EMAIL:}") private String bootstrapEmail;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapEmail.isBlank()) return;

        userProfileRepository.findByEmail(bootstrapEmail).ifPresent(user -> {
            if (user.isEmailConfirmed()) return;

            boolean hasToken = emailConfirmationTokenRepository
                    .findAll().stream()
                    .anyMatch(t -> t.getUserId().equals(user.getId()) && !t.isUsed());
            if (hasToken) return;

            String token = UUID.randomUUID().toString();
            emailConfirmationTokenRepository.save(EmailConfirmationToken.builder()
                    .userId(user.getId())
                    .token(token)
                    .expiresAt(OffsetDateTime.now().plusHours(24))
                    .used(false)
                    .createdAt(OffsetDateTime.now())
                    .build());

            emailService.sendConfirmationEmail(bootstrapEmail, token);
            log.info("Sent confirmation email to bootstrap admin: {}", bootstrapEmail);
        });
    }
}
