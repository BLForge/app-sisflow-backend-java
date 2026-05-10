package io.snortexware.sisflow.services;

import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.repositories.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TenantResolver {

    private final TenantRepository tenantRepository;

    @Value("${app.base.domain:localhost}")
    private String baseDomain;

    /**
     * Resolves the tenant from the Host header subdomain.
     * Returns empty if the host doesn't match the base domain pattern or no tenant found.
     */
    public Optional<Tenant> resolveFromRequest(HttpServletRequest request) {
        String hostname = request.getServerName();
        if (hostname == null || !hostname.endsWith("." + baseDomain)) return Optional.empty();
        String subdomain = hostname.substring(0, hostname.length() - baseDomain.length() - 1);
        return tenantRepository.findByDomain(subdomain);
    }
}
