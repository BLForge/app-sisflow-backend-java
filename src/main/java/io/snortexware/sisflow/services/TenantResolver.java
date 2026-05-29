package io.snortexware.sisflow.services;

import io.snortexware.sisflow.entities.Tenant;
import io.snortexware.sisflow.repositories.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantResolver {

    private final TenantRepository tenantRepository;

    @Value("${app.base.domain:localhost}")
    private String baseDomain;

    public Optional<Tenant> resolveFromRequest(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        boolean fromTrustedProxy = "127.0.0.1".equals(remoteAddr) || "::1".equals(remoteAddr)
                || "0:0:0:0:0:0:0:1".equals(remoteAddr);

        String hostname;
        if (fromTrustedProxy) {
            hostname = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
            if (hostname == null || hostname.isBlank()) {
                hostname = request.getHeader("Host");
            }
        } else {
            hostname = request.getServerName();
        }

        if (hostname == null || hostname.isBlank()) return Optional.empty();

        hostname = hostname.split(":")[0];

        if (!hostname.endsWith("." + baseDomain)) return Optional.empty();

        String subdomain = hostname.substring(0, hostname.length() - baseDomain.length() - 1);
        if (subdomain.isBlank() || subdomain.contains(".")) return Optional.empty();

        log.debug("Resolved tenant subdomain: {} from hostname: {}", subdomain, hostname);
        return tenantRepository.findByDomain(subdomain);
    }

    private String firstHeaderValue(String value) {
        if (value == null || value.isBlank()) return value;
        return value.split(",")[0].trim();
    }
}
