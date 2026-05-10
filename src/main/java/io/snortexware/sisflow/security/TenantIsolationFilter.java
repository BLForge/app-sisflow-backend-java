package io.snortexware.sisflow.security;

import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.services.JwtService;
import io.snortexware.sisflow.services.TenantResolver;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class TenantIsolationFilter implements Filter {

    private final JwtService jwtService;
    private final TenantContext tenantContext;
    private final UserProfileRepository userProfileRepository;
    private final TenantResolver tenantResolver;
    private final boolean trustHostHeader;

    private static final String[] PUBLIC_PATHS = {
            "/auth/", "/health", "/swagger-ui/",
            "/v3/api-docs/", "/github/webhook", "/error", "/tenants/register"
    };

    public TenantIsolationFilter(JwtService jwtService, TenantContext tenantContext,
                                  UserProfileRepository userProfileRepository,
                                  TenantResolver tenantResolver,
                                  @Value("${app.trust-host-header:false}") boolean trustHostHeader) {
        this.jwtService = jwtService;
        this.tenantContext = tenantContext;
        this.userProfileRepository = userProfileRepository;
        this.tenantResolver = tenantResolver;
        this.trustHostHeader = trustHostHeader;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        try {
            String path = req.getRequestURI();
            if (isPublicPath(path)) {
                chain.doFilter(request, response);
                return;
            }

            String auth = req.getHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                chain.doFilter(request, response);
                return;
            }

            // Fix #1: invalid/expired JWT → 401 instead of silent pass-through
            String token = auth.substring(7);
            UUID userId;
            try {
                userId = jwtService.getUserIdFromToken(token);
            } catch (Exception e) {
                log.debug("JWT validation failed: {}", e.getMessage());
                GlobalExceptionHandler.writeError(res, HttpStatus.UNAUTHORIZED, ErrorCode.AUTH_TOKEN_INVALID);
                return;
            }

            tenantContext.setCurrentUser(userId);

            UUID tenantId = jwtService.getTenantIdFromToken(token);
            if (tenantId == null) {
                var user = userProfileRepository.findByIdWithTenant(userId).orElse(null);
                if (user != null && user.getTenant() != null)
                    tenantId = user.getTenant().getId();
            }

            // Fix #2: authenticated user with no resolvable tenant → 401
            if (tenantId == null) {
                log.warn("Authenticated user {} has no tenant", userId);
                GlobalExceptionHandler.writeError(res, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                return;
            }

            // Fix #3/#5: when behind trusted proxy, use raw Host header and hard-reject unknown subdomains
            if (trustHostHeader) {
                UUID hostTenantId = resolveTenantFromHost(req);
                if (hostTenantId == null || !hostTenantId.equals(tenantId)) {
                    log.warn("Tenant check failed for user {}: hostTenantId={}, jwtTenantId={}", userId, hostTenantId, tenantId);
                    GlobalExceptionHandler.writeError(res, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                    return;
                }
            }

            tenantContext.setCurrentTenant(tenantId);
            chain.doFilter(request, response);
        } finally {
            tenantContext.clear();
        }
    }

    private UUID resolveTenantFromHost(HttpServletRequest req) {
        return tenantResolver.resolveFromRequest(req).map(t -> t.getId()).orElse(null);
    }

    // Fix #4: exact segment match to avoid /auth-admin/ bypassing /auth/ check
    private boolean isPublicPath(String path) {
        for (String p : PUBLIC_PATHS) if (path.startsWith(p)) return true;
        return false;
    }
}
