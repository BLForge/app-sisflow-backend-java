package io.snortexware.sisflow.security;

import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.security.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ThreadLocal-based tenant and user context management.
 * Ensures tenant isolation and provides context for authorization checks.
 */
@Slf4j
@Component
public class TenantContext {

    private static final ThreadLocal<UUID> tenantContext = new ThreadLocal<>();
    private static final ThreadLocal<UUID> userContext = new ThreadLocal<>();
    private static final ThreadLocal<UserProfile> userProfileContext = new ThreadLocal<>();

    /**
     * Set the current tenant ID for this thread.
     */
    public void setCurrentTenant(UUID tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID cannot be null");
        }
        tenantContext.set(tenantId);
        log.debug("Tenant context set to: {}", tenantId);
    }

    /**
     * Get the current tenant ID for this thread.
     */
    public UUID getCurrentTenant() {
        return tenantContext.get(); // null means no tenant context (system_admin)
    }

    /**
     * Check if tenant context is set.
     */
    public boolean hasTenant() {
        return tenantContext.get() != null;
    }

    /**
     * Clear the tenant context for this thread.
     */
    public void clearTenant() {
        tenantContext.remove();
        log.debug("Tenant context cleared");
    }

    /**
     * Set the current user ID for this thread.
     */
    public void setCurrentUser(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        userContext.set(userId);
        log.debug("User context set to: {}", userId);
    }

    /**
     * Get the current user ID for this thread.
     */
    public UUID getCurrentUser() {
        return userContext.get(); // null means unauthenticated
    }

    /**
     * Check if user context is set.
     */
    public boolean hasUser() {
        return userContext.get() != null;
    }

    /**
     * Set the current user profile for this thread.
     */
    public void setCurrentUserProfile(UserProfile userProfile) {
        if (userProfile == null) {
            throw new IllegalArgumentException("User profile cannot be null");
        }
        userProfileContext.set(userProfile);
    }

    /**
     * Get the current user profile for this thread.
     */
    public UserProfile getCurrentUserProfile() {
        UserProfile profile = userProfileContext.get();
        if (profile == null) {
            throw new UnauthorizedException("No user profile context found. Request must be authenticated.");
        }
        return profile;
    }

    /**
     * Check if user profile context is set.
     */
    public boolean hasUserProfile() {
        return userProfileContext.get() != null;
    }

    /**
     * Clear all context for this thread.
     */
    public void clear() {
        tenantContext.remove();
        userContext.remove();
        userProfileContext.remove();
        log.debug("All context cleared");
    }

    /**
     * Validate that the given tenant ID matches the current tenant context.
     */
    public void validateTenantAccess(UUID tenantId) {
        UUID currentTenant = getCurrentTenant();
        if (!currentTenant.equals(tenantId)) {
            log.warn("Tenant access violation: user from tenant {} tried to access tenant {}", 
                currentTenant, tenantId);
            throw new UnauthorizedException("Access denied: tenant mismatch");
        }
    }

    /**
     * Validate that the given user belongs to the current tenant.
     */
    public void validateUserInTenant(UUID userId, UUID tenantId) {
        UUID currentTenant = getCurrentTenant();
        if (!currentTenant.equals(tenantId)) {
            log.warn("User tenant validation failed: user {} from tenant {} tried to access tenant {}", 
                userId, currentTenant, tenantId);
            throw new UnauthorizedException("Access denied: user not in tenant");
        }
    }
}
