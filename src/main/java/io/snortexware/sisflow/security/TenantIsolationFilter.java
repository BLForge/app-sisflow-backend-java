package io.snortexware.sisflow.security;

import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.services.JwtService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class TenantIsolationFilter implements Filter {

    private final JwtService jwtService;
    private final TenantContext tenantContext;
    private final TenantRepository tenantRepository;

    private static final String[] PUBLIC_PATHS = {
            "/auth/", "/health", "/files/", "/swagger-ui/",
            "/v3/api-docs/", "/github/webhook", "/error", "/tenants/register"
    };

    public TenantIsolationFilter(JwtService jwtService, TenantContext tenantContext, TenantRepository tenantRepository) {
        this.jwtService = jwtService;
        this.tenantContext = tenantContext;
        this.tenantRepository = tenantRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        try {
            String path = req.getRequestURI();
            if (isPublicPath(path)) {
                chain.doFilter(request, response);
                return;
            }

            // Resolve tenant from subdomain
            String domain = extractSubdomain(req);
            if (domain != null) {
                tenantRepository.findByDomain(domain).ifPresent(t ->
                        tenantContext.setCurrentTenant(t.getId()));
            }

            // Resolve user from JWT
            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                try {
                    UUID userId = jwtService.getUserIdFromToken(auth.substring(7));
                    if (userId != null) tenantContext.setCurrentUser(userId);
                } catch (Exception e) {
                    log.debug("JWT parse failed: {}", e.getMessage());
                }
            }

            chain.doFilter(request, response);
        } finally {
            tenantContext.clear();
            log.debug("Context cleared after request");
        }
    }

    private String extractSubdomain(HttpServletRequest req) {
        // 1. Explicit header (set by frontend)
        String header = req.getHeader("X-Tenant-Domain");
        if (header != null && !header.isBlank()) return header.trim();

        // 2. Extract from Host: acme.localhost → acme, acme.sisflow.com → acme
        String host = req.getHeader("Host");
        if (host != null) {
            host = host.split(":")[0];
            String[] parts = host.split("\\.");
            if (parts.length >= 2) {
                String sub = parts[0];
                if (!sub.equals("www") && !sub.equals("localhost") && !sub.equals("sisflow")) {
                    return sub;
                }
            }
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        for (String p : PUBLIC_PATHS) if (path.startsWith(p)) return true;
        return false;
    }
}
