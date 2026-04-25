package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.services.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Diagnostic controller to check RLS and migration status.
 * Remove this in production.
 */
@Slf4j
@RestController
@RequestMapping("/diagnostic")
public class DiagnosticController {

    private final TenantContext tenantContext;
    private final AuthorizationService authorizationService;
    private final JdbcTemplate jdbcTemplate;

    public DiagnosticController(
            TenantContext tenantContext,
            AuthorizationService authorizationService,
            JdbcTemplate jdbcTemplate
    ) {
        this.tenantContext = tenantContext;
        this.authorizationService = authorizationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus(@AuthenticationPrincipal UUID callerId) {
        Map<String, Object> status = new HashMap<>();
        
        // SECURITY: Only admins can access diagnostic information
        if (callerId == null) {
            status.put("error", "Authentication required");
            return status;
        }
        
        try {
            if (!authorizationService.isAdminOrAbove(callerId)) {
                status.put("error", "Admin access required");
                log.warn("Non-admin user {} attempted to access diagnostic status", callerId);
                return status;
            }
        } catch (Exception e) {
            status.put("error", "Authorization check failed");
            log.error("Authorization check failed for user: {}", callerId, e);
            return status;
        }
        
        try {
            // Check current user
            UUID currentUser = tenantContext.hasUser() ? tenantContext.getCurrentUser() : null;
            status.put("currentUser", currentUser);
            
            if (currentUser != null) {
                // Check user hierarchy level
                Integer hierarchyLevel = authorizationService.getCurrentUserHierarchyLevel(currentUser);
                status.put("hierarchyLevel", hierarchyLevel);
                
                // Check if user is admin
                boolean isAdmin = authorizationService.isAdminOrAbove(currentUser);
                status.put("isAdmin", isAdmin);
                
                // Check user roles
                var roles = authorizationService.getUserRoles(currentUser);
                status.put("roles", roles.size());
                
                // Check user permissions
                var permissions = authorizationService.getUserPermissions(currentUser);
                status.put("permissions", permissions.size());
            }
            
            // Check if RLS functions exist
            try {
                String functionCheck = "SELECT routine_name FROM information_schema.routines WHERE routine_name = 'get_current_user_id'";
                List<String> functions = jdbcTemplate.queryForList(functionCheck, String.class);
                status.put("rlsFunctionsExist", !functions.isEmpty());
            } catch (Exception e) {
                status.put("rlsFunctionsExist", false);
                status.put("rlsFunctionError", "Database check failed");
            }
            
            // Check if visibility columns exist
            try {
                String columnCheck = "SELECT column_name FROM information_schema.columns WHERE table_name = 'tickets' AND column_name = 'visibility'";
                List<String> columns = jdbcTemplate.queryForList(columnCheck, String.class);
                status.put("visibilityColumnsExist", !columns.isEmpty());
            } catch (Exception e) {
                status.put("visibilityColumnsExist", false);
                status.put("visibilityColumnError", "Database check failed");
            }
            
            // Check RLS status on tables
            try {
                String rlsCheck = "SELECT tablename, rowsecurity FROM pg_tables WHERE tablename IN ('tickets', 'customers') AND schemaname = 'public'";
                List<Map<String, Object>> rlsStatus = jdbcTemplate.queryForList(rlsCheck);
                status.put("rlsEnabled", rlsStatus);
            } catch (Exception e) {
                status.put("rlsEnabled", false);
                status.put("rlsError", "Database check failed");
            }
            
            // Check latest migrations
            try {
                String migrationCheck = "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5";
                List<Map<String, Object>> migrations = jdbcTemplate.queryForList(migrationCheck);
                status.put("latestMigrations", migrations);
            } catch (Exception e) {
                status.put("latestMigrations", "Database check failed");
            }
            
            // Test RLS context setting - FIXED SQL INJECTION
            try {
                if (currentUser != null) {
                    jdbcTemplate.update("SET app.current_user_id = ?", currentUser.toString());
                    String contextCheck = "SELECT current_setting('app.current_user_id', true)";
                    String contextValue = jdbcTemplate.queryForObject(contextCheck, String.class);
                    status.put("rlsContextSet", contextValue != null && !contextValue.isEmpty());
                } else {
                    status.put("rlsContextSet", false);
                }
            } catch (Exception e) {
                status.put("rlsContextSet", false);
                status.put("rlsContextError", "Context check failed");
            }
            
        } catch (Exception e) {
            status.put("error", "Diagnostic check failed");
            log.error("Diagnostic error for admin user: {}", callerId, e);
        }
        
        return status;
    }
    
    @GetMapping("/test-rls")
    public Map<String, Object> testRLS(@AuthenticationPrincipal UUID callerId) {
        Map<String, Object> result = new HashMap<>();
        
        // SECURITY: Only admins can test RLS
        if (callerId == null) {
            result.put("error", "Authentication required");
            return result;
        }
        
        try {
            if (!authorizationService.isAdminOrAbove(callerId)) {
                result.put("error", "Admin access required");
                log.warn("Non-admin user {} attempted to test RLS", callerId);
                return result;
            }
        } catch (Exception e) {
            result.put("error", "Authorization check failed");
            log.error("Authorization check failed for user: {}", callerId, e);
            return result;
        }
        
        try {
            UUID currentUser = tenantContext.hasUser() ? tenantContext.getCurrentUser() : null;
            
            if (currentUser == null) {
                result.put("error", "No authenticated user");
                return result;
            }
            
            // Set RLS context - FIXED SQL INJECTION
            jdbcTemplate.update("SET app.current_user_id = ?", currentUser.toString());
            
            // Test RLS functions
            try {
                String userIdResult = jdbcTemplate.queryForObject("SELECT get_current_user_id()", String.class);
                result.put("getCurrentUserId", userIdResult);
            } catch (Exception e) {
                result.put("getCurrentUserIdError", "Function test failed");
            }
            
            try {
                Integer hierarchyLevel = jdbcTemplate.queryForObject("SELECT get_current_user_hierarchy_level()", Integer.class);
                result.put("getHierarchyLevel", hierarchyLevel);
            } catch (Exception e) {
                result.put("getHierarchyLevelError", "Function test failed");
            }
            
            try {
                Boolean isAdmin = jdbcTemplate.queryForObject("SELECT is_admin_or_above()", Boolean.class);
                result.put("isAdminOrAbove", isAdmin);
            } catch (Exception e) {
                result.put("isAdminOrAboveError", "Function test failed");
            }
            
            // Test ticket count with and without RLS
            try {
                Integer totalTickets = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tickets", Integer.class);
                result.put("totalTickets", totalTickets);
            } catch (Exception e) {
                result.put("totalTicketsError", "Query test failed");
            }
            
        } catch (Exception e) {
            result.put("error", "RLS test failed");
            log.error("RLS test error for admin user: {}", callerId, e);
        }
        
        return result;
    }
}