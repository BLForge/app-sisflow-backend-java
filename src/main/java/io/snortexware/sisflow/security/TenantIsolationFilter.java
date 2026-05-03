package io.snortexware.sisflow.security;

import io.snortexware.sisflow.repositories.UserProfileRepository;
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
    private final UserProfileRepository userProfileRepository;

    private static final String[] PUBLIC_PATHS = {
            "/auth/", "/health", "/files/upload", "/swagger-ui/",
            "/v3/api-docs/", "/github/webhook", "/error", "/tenants/register"
    };

    public TenantIsolationFilter(JwtService jwtService, TenantContext tenantContext,
                                  UserProfileRepository userProfileRepository) {
        this.jwtService = jwtService;
        this.tenantContext = tenantContext;
        this.userProfileRepository = userProfileRepository;
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
                try {
                    UUID userId = jwtService.getUserIdFromToken(auth.substring(7));
                    if (userId != null) {
                        tenantContext.setCurrentUser(userId);
                        // Derive tenant from the user's own DB record — never trust client headers
                        userProfileRepository.findById(userId).ifPresent(user -> {
                            if (user.getTenant() != null) {
                                tenantContext.setCurrentTenant(user.getTenant().getId());
                            }
                        });
                    }
                } catch (Exception e) {
                    log.debug("JWT parse failed: {}", e.getMessage());
                }
            }

            chain.doFilter(request, response);
        } finally {
            tenantContext.clear();
        }
    }

    private boolean isPublicPath(String path) {
        for (String p : PUBLIC_PATHS) if (path.startsWith(p)) return true;
        return false;
    }
}
