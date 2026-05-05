package io.snortexware.sisflow.security;

import io.snortexware.sisflow.repositories.TenantRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.services.JwtService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Slf4j
@Component
public class TenantIsolationFilter implements Filter {

    private final JwtService jwtService;
    private final TenantContext tenantContext;
    private final UserProfileRepository userProfileRepository;
    private final TenantRepository tenantRepository;

    private static final String[] PUBLIC_PATHS = {
            "/auth/", "/health", "/swagger-ui/",
            "/v3/api-docs/", "/github/webhook", "/error", "/tenants/register"
    };

    public TenantIsolationFilter(JwtService jwtService, TenantContext tenantContext,
                                  UserProfileRepository userProfileRepository,
                                  TenantRepository tenantRepository) {
        this.jwtService = jwtService;
        this.tenantContext = tenantContext;
        this.userProfileRepository = userProfileRepository;
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

            String auth = req.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                UUID userId = null;
                try {
                    userId = jwtService.getUserIdFromToken(auth.substring(7));
                } catch (Exception e) {
                    log.debug("JWT parse failed: {}", e.getMessage());
                }

                if (userId != null) {
                    tenantContext.setCurrentUser(userId);
                    var user = userProfileRepository.findById(userId).orElse(null);
                    if (user != null && user.getTenant() != null) {
                        String requestDomain = extractDomain(req);
                        String userTenantDomain = user.getTenant().getDomain();
                        if (requestDomain != null && !requestDomain.isEmpty()
                                && !requestDomain.equals(userTenantDomain)
                                && tenantRepository.findByDomain(requestDomain).isPresent()) {
                            log.warn("Tenant mismatch: user {} (tenant={}) tried domain {}",
                                    userId, userTenantDomain, requestDomain);
                            ((HttpServletResponse) response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
                            return;
                        }
                        tenantContext.setCurrentTenant(user.getTenant().getId());
                    }
                }
            }

            chain.doFilter(request, response);
        } finally {
            tenantContext.clear();
        }
    }

    /**
     * Extract the hostname from the Origin or Referer header, falling back to Host.
     */
    private String extractDomain(HttpServletRequest req) {
        String origin = req.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            try {
                return URI.create(origin).getHost();
            } catch (Exception ignored) {}
        }
        String referer = req.getHeader("Referer");
        if (referer != null && !referer.isEmpty()) {
            try {
                return URI.create(referer).getHost();
            } catch (Exception ignored) {}
        }
        // Strip port from Host header
        String host = req.getHeader("Host");
        if (host != null) return host.split(":")[0];
        return null;
    }

    private boolean isPublicPath(String path) {
        for (String p : PUBLIC_PATHS) if (path.startsWith(p)) return true;
        return false;
    }

    private static class TenantMismatchException extends RuntimeException {}
}
