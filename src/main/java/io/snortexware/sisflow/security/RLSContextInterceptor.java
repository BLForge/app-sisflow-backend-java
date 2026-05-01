package io.snortexware.sisflow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Slf4j
@Component
public class RLSContextInterceptor implements HandlerInterceptor {

    private final TenantContext tenantContext;
    private final JdbcTemplate jdbcTemplate;

    public RLSContextInterceptor(TenantContext tenantContext, JdbcTemplate jdbcTemplate) {
        this.tenantContext = tenantContext;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // Check if tenant context is set (meaning user is authenticated)
            if (tenantContext.hasUser()) {
                UUID userId = tenantContext.getCurrentUser();
                
                // Set RLS context variables in PostgreSQL session
                String userIdSql = "SET app.current_user_id = '" + userId.toString() + "'";
                String roleSql = "SET app.current_user_role = '" + getUserRoleString(userId) + "'";
                
                try {
                    jdbcTemplate.execute(userIdSql);
                    jdbcTemplate.execute(roleSql);
                    log.debug("RLS context set for user: {}", userId);
                } catch (Exception e) {
                    log.warn("Failed to set RLS context variables: {}", e.getMessage());
                    // Continue anyway - RLS will use default behavior
                }
            }
        } catch (Exception e) {
            log.debug("Error setting RLS context: {}", e.getMessage());
            // Continue without RLS context
        }

        return true;
    }

    /**
     * Get the user's highest role code for RLS context.
     */
    private String getUserRoleString(UUID userId) {
        try {
            Integer level = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(r.hierarchy_level), -1) FROM user_roles ur " +
                "JOIN roles r ON r.id = ur.role_id WHERE ur.user_id = ? AND ur.is_active = true",
                Integer.class, userId);
            if (level == null || level < 0) return "client";
            if (level >= 4) return "system_admin";
            if (level >= 3) return "tenant_admin";
            if (level >= 2) return "moderator";
            if (level >= 1) return "developer";
            return "client";
        } catch (Exception e) {
            log.debug("Error getting user role: {}", e.getMessage());
            return "client";
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        // Context is cleared by TenantIsolationFilter, but we can add additional cleanup here if needed
    }
}
