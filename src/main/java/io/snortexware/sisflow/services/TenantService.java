package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.TenantRegistrationRequest;
import io.snortexware.sisflow.entities.EmailConfirmationToken;
import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.EmailConfirmationTokenRepository;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final EmailConfirmationTokenRepository emailConfirmationTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional
    public void register(TenantRegistrationRequest req) {
        if (tenantRepository.findByDomain(req.getDomain()).isPresent())
            throw AppException.conflict();

        if (userProfileRepository.findByEmail(req.getAdminEmail()).isPresent())
            throw AppException.conflict();

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(req.getTenantName())
                .domain(req.getDomain())
                .status(Tenant.Status.active)
                .createdAt(OffsetDateTime.now())
                .build());

        UserProfile admin = UserProfile.builder()
                .id(UUID.randomUUID())
                .tenant(tenant)
                .email(req.getAdminEmail())
                .passwordHash(passwordEncoder.encode(req.getAdminPassword()))
                .name(req.getAdminName())
                .type(UserProfile.UserType.admin)
                .role(UserProfile.Role.admin)
                .emailConfirmed(false)
                .createdAt(OffsetDateTime.now())
                .build();

        userProfileRepository.saveAndFlush(admin);

        roleRepository.findByCode("tenant_admin").ifPresent(role ->
                userRoleRepository.save(UserRole.builder()
                        .user(admin).role(role).isActive(true).assignedAt(OffsetDateTime.now()).build()));

        String token = UUID.randomUUID().toString();
        emailConfirmationTokenRepository.save(EmailConfirmationToken.builder()
                .userId(admin.getId())
                .token(token)
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .used(false)
                .createdAt(OffsetDateTime.now())
                .build());

        emailService.sendConfirmationEmail(req.getAdminEmail(), token);
    }
}
