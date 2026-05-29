package io.snortexware.sisflow.auth.infrastructure.tenant;

import io.snortexware.sisflow.auth.application.AuthPersistence;
import io.snortexware.sisflow.auth.application.port.TenantResolutionPort;
import io.snortexware.sisflow.auth.domain.model.Tenant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class HostTenantResolver implements TenantResolutionPort {

    private final AuthPersistence authPersistence;

    @Value("${app.base.domain:localhost}")
    private String baseDomain;

    @Override
    public Optional<Tenant> resolve(HttpServletRequest request) {
        String hostname = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        if (hostname == null || hostname.isBlank()) {
            hostname = request.getHeader("Host");
        }
        if (hostname == null || hostname.isBlank()) {
            hostname = request.getServerName();
        }
        if (hostname == null || hostname.isBlank()) {
            return Optional.empty();
        }

        hostname = hostname.split(":")[0];
        if (!hostname.endsWith("." + baseDomain)) {
            return Optional.empty();
        }

        String subdomain = hostname.substring(0, hostname.length() - baseDomain.length() - 1);
        if (subdomain.isBlank() || subdomain.contains(".")) {
            return Optional.empty();
        }

        return authPersistence.findTenantByDomain(subdomain);
    }

    private String firstHeaderValue(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.split(",")[0].trim();
    }
}
