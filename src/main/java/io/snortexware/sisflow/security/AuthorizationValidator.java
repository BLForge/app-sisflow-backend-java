package io.snortexware.sisflow.security;

import io.snortexware.sisflow.security.exceptions.AccessDeniedException;
import io.snortexware.sisflow.security.exceptions.UnauthorizedException;
import io.snortexware.sisflow.services.PermissionService;
import io.snortexware.sisflow.services.RoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Component for validating user authorization based on roles and permissions.
 */
@Slf4j
@Component
public class AuthorizationValidator {

    private final RoleService roleService;
    private final PermissionService permissionService;
    private final TenantContext tenantContext;

    public AuthorizationValidator(
            RoleService roleService,
            PermissionService permissionService,
            TenantContext tenantContext) {
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.tenantContext = tenantContext;
    }

    /**
     * Require a specific role for the current user.
     * Throws AccessDeniedException if user doesn't have the role.
     */
    public void requireRole(String roleCode) {
        UUID userId = tenantContext.getCurrentUser();
        if (!roleService.userHasRole(userId, roleCode)) {
            log.warn("User {} does not have required role: {}", userId, roleCode);
            throw new AccessDeniedException(
                    "Access denied: required role '" + roleCode + "' not found",
                    "INSUFFICIENT_ROLE",
                    roleCode);
        }
    }

    /**
     * Require any of the specified roles for the current user.
     * Throws AccessDeniedException if user doesn't have any of the roles.
     */
    public void requireAnyRole(List<String> roleCodes) {
        UUID userId = tenantContext.getCurrentUser();
        boolean hasAnyRole = roleCodes.stream()
                .anyMatch(roleCode -> roleService.userHasRole(userId, roleCode));

        if (!hasAnyRole) {
            log.warn("User {} does not have any of the required roles: {}", userId, roleCodes);
            throw new AccessDeniedException(
                    "Access denied: none of the required roles found",
                    "INSUFFICIENT_ROLE",
                    String.join(", ", roleCodes));
        }
    }

    /**
     * Require all of the specified roles for the current user.
     * Throws AccessDeniedException if user doesn't have all the roles.
     */
    public void requireAllRoles(List<String> roleCodes) {
        UUID userId = tenantContext.getCurrentUser();
        boolean hasAllRoles = roleCodes.stream()
                .allMatch(roleCode -> roleService.userHasRole(userId, roleCode));

        if (!hasAllRoles) {
            log.warn("User {} does not have all required roles: {}", userId, roleCodes);
            throw new AccessDeniedException(
                    "Access denied: not all required roles found",
                    "INSUFFICIENT_ROLE",
                    String.join(", ", roleCodes));
        }
    }

    /**
     * Require a specific permission for the current user.
     * Throws AccessDeniedException if user doesn't have the permission.
     */
    public void requirePermission(String permissionCode) {
        UUID userId = tenantContext.getCurrentUser();
        if (!permissionService.hasPermission(userId, permissionCode)) {
            log.warn("User {} does not have required permission: {}", userId, permissionCode);
            throw new AccessDeniedException(
                    "Access denied: required permission '" + permissionCode + "' not found",
                    "INSUFFICIENT_PERMISSION",
                    permissionCode);
        }
    }

    /**
     * Require any of the specified permissions for the current user.
     * Throws AccessDeniedException if user doesn't have any of the permissions.
     */
    public void requireAnyPermission(List<String> permissionCodes) {
        UUID userId = tenantContext.getCurrentUser();
        if (!permissionService.hasAnyPermission(userId, permissionCodes)) {
            log.warn("User {} does not have any of the required permissions: {}", userId, permissionCodes);
            throw new AccessDeniedException(
                    "Access denied: none of the required permissions found",
                    "INSUFFICIENT_PERMISSION",
                    String.join(", ", permissionCodes));
        }
    }

    /**
     * Require all of the specified permissions for the current user.
     * Throws AccessDeniedException if user doesn't have all the permissions.
     */
    public void requireAllPermissions(List<String> permissionCodes) {
        UUID userId = tenantContext.getCurrentUser();
        if (!permissionService.hasAllPermissions(userId, permissionCodes)) {
            log.warn("User {} does not have all required permissions: {}", userId, permissionCodes);
            throw new AccessDeniedException(
                    "Access denied: not all required permissions found",
                    "INSUFFICIENT_PERMISSION",
                    String.join(", ", permissionCodes));
        }
    }

    /**
     * Require access to a specific resource.
     * Throws AccessDeniedException if user doesn't have access.
     */
    public void requireResourceAccess(UUID resourceId, String action) {
        UUID userId = tenantContext.getCurrentUser();
        if (!permissionService.canAccessResource(userId, resourceId, action)) {
            log.warn("User {} does not have access to resource {} with action {}", userId, resourceId, action);
            throw new AccessDeniedException(
                    "Access denied: insufficient permissions for resource access",
                    "INSUFFICIENT_RESOURCE_ACCESS",
                    action);
        }
    }

    /**
     * Require access to a specific ticket.
     * Throws AccessDeniedException if user doesn't have access.
     */
    public void requireTicketAccess(UUID ticketId, String action) {
        UUID userId = tenantContext.getCurrentUser();
        if (!permissionService.canAccessTicket(userId, ticketId, action)) {
            log.warn("User {} does not have access to ticket {} with action {}", userId, ticketId, action);
            throw new AccessDeniedException(
                    "Access denied: insufficient permissions for ticket access",
                    "INSUFFICIENT_TICKET_ACCESS",
                    action);
        }
    }
}
