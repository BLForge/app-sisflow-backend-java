package io.snortexware.sisflow.security;

import io.snortexware.sisflow.security.exceptions.UnauthorizedException;
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
import java.util.UUID;

/**
 * Filter to extract user ID from JWT token and set context.
 * Uses a default tenant ID since Supabase tokens don't contain tenant information.
 * Skips public endpoints that don't require authentication.
 */
@Slf4j
@Component
public class TenantIsolationFilter implements Filter {

    private final JwtService jwtService;
    private final TenantContext tenantContext;

    // Public endpoints that don't require authentication context
    private static final String[] PUBLIC_PATHS = {
            "/auth/",
            "/health",
            "/files/",
            "/swagger-ui/",
            "/v3/api-docs/",
            "/github/webhook",
            "/error"
    };

    public TenantIsolationFilter(JwtService jwtService, TenantContext tenantContext) {
        this.jwtService = jwtService;
        this.tenantContext = tenantContext;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Skip tenant context for public endpoints
            String requestPath = httpRequest.getRequestURI();
            if (isPublicPath(requestPath)) {
                log.debug("Skipping authentication context for public path: {}", requestPath);
                chain.doFilter(request, response);
                return;
            }

            // Extract tenant ID from JWT token
            String authHeader = httpRequest.getHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                
                try {
                    // Extract user ID from token
                    UUID userId = jwtService.getUserIdFromToken(token);
                    
                    if (userId != null) {
                        // Set user context
                        tenantContext.setCurrentUser(userId);
                        
                        // For now, use a default tenant ID since Supabase tokens don't contain tenant info
                        // TODO: Implement proper tenant management
                        UUID defaultTenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
                        tenantContext.setCurrentTenant(defaultTenantId);
                        
                        log.debug("Context set for request: tenant={} (default), user={}", defaultTenantId, userId);
                    } else {
                        log.debug("User ID is null in token");
                    }
                } catch (Exception e) {
                    log.debug("Failed to extract user ID from token: {}", e.getMessage());
                    // Continue without context - will be caught by authorization checks
                }
            } else {
                log.debug("No Authorization header found for path: {}", requestPath);
            }
            
            // Continue with the filter chain
            chain.doFilter(request, response);
            
        } finally {
            // Clear context after request is processed
            tenantContext.clear();
            log.debug("Context cleared after request");
        }
    }

    /**
     * Check if the request path is a public endpoint that doesn't require authentication context.
     */
    private boolean isPublicPath(String requestPath) {
        for (String publicPath : PUBLIC_PATHS) {
            if (requestPath.startsWith(publicPath)) {
                return true;
            }
        }
        return false;
    }
}
