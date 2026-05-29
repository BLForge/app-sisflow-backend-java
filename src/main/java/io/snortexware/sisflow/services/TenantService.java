package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.TenantRegistrationRequest;
import io.snortexware.sisflow.dto.UpdateTenantBrandingRequest;
import io.snortexware.sisflow.entities.EmailConfirmationToken;
import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.EmailConfirmationTokenRepository;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
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
    private final AuthorizationService authorizationService;
    private final TenantContext tenantContext;

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

    @Transactional
    public Tenant updateBranding(UUID callerId, UpdateTenantBrandingRequest req) {
        UUID tenantId = tenantContext.getCurrentTenant();
        if (tenantId == null) throw AppException.unauthorized();

        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(AppException::notFound);

        if (req.getName() != null && !req.getName().isBlank()) {
            if (authorizationService.getCurrentUserHierarchyLevel(callerId) < 4) {
                throw AppException.forbidden();
            }
            if (req.getName().length() > 100) throw AppException.badRequest();
            tenant.setName(req.getName());
        }
        if (req.getLogoUrl() != null && !req.getLogoUrl().isBlank()) {
            validateFileUrl(req.getLogoUrl());
            tenant.setLogoUrl(req.getLogoUrl());
        }
        if (req.getLogoIconUrl() != null && !req.getLogoIconUrl().isBlank()) {
            validateFileUrl(req.getLogoIconUrl());
            tenant.setLogoIconUrl(req.getLogoIconUrl());
        }
        if (req.getBackgroundUrl() != null && !req.getBackgroundUrl().isBlank()) {
            validateFileUrl(req.getBackgroundUrl());
            tenant.setBackgroundUrl(req.getBackgroundUrl());
        }

        tenant.setUpdatedAt(OffsetDateTime.now());
        return tenantRepository.save(tenant);
    }

    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    private void validateFileUrl(String fileUrl) {
        if (!fileUrl.startsWith("/files/")) {
            throw AppException.badRequest();
        }
    }
}
