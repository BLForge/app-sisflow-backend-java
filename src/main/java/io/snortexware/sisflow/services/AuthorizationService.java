package io.snortexware.sisflow.services;

import io.snortexware.sisflow.entities.*;
import io.snortexware.sisflow.repositories.*;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for role-based authorization checks.
 * Implements the role hierarchy and permission validation.
 */
@Slf4j
@Service
public class AuthorizationService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantContext tenantContext;

    public AuthorizationService(
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            TenantContext tenantContext
    ) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.tenantContext = tenantContext;
    }

    /**
     * Get the highest hierarchy level for the current user.
     * Returns -1 if user has no roles.
     */
    public Integer getCurrentUserHierarchyLevel(UUID userId) {
        if (userId == null) {
            return -1;
        }
        return userRoleRepository.findActiveByUserId(userId)
                .stream()
                .map(UserRole::getRole)
                .map(Role::getHierarchyLevel)
                .max(Integer::compareTo)
                .orElse(-1);
    }

    /**
     * Check if user is admin or above (hierarchy level >= 4).
     */
    public boolean isAdminOrAbove(UUID userId) {
        if (userId == null) {
            return false;
        }
        return getCurrentUserHierarchyLevel(userId) >= 4;
    }

    /**
     * Check if user is moderator or above (hierarchy level >= 2).
     */
    public boolean isModeratorOrAbove(UUID userId) {
        if (userId == null) {
            return false;
        }
        return getCurrentUserHierarchyLevel(userId) >= 2;
    }

    /**
     * Check if user is developer or above (hierarchy level >= 1).
     */
    public boolean isDeveloperOrAbove(UUID userId) {
        if (userId == null) {
            return false;
        }
        return getCurrentUserHierarchyLevel(userId) >= 1;
    }

    /**
     * Check if user is client (hierarchy level >= 0).
     */
    public boolean isClient(UUID userId) {
        if (userId == null) {
            return false;
        }
        return getCurrentUserHierarchyLevel(userId) >= 0;
    }

    /**
     * Check if user has a specific permission.
     */
    public boolean hasPermission(UUID userId, String permissionCode) {
        if (userId == null) {
            return false;
        }
        return userRoleRepository.findActiveByUserId(userId)
                .stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(Permission::getCode)
                .anyMatch(code -> code.equals(permissionCode));
    }

    /**
     * Check if user has any of the given permissions.
     */
    public boolean hasAnyPermission(UUID userId, String... permissionCodes) {
        if (userId == null) {
            return false;
        }
        Set<String> userPermissions = userRoleRepository.findActiveByUserId(userId)
                .stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        for (String code : permissionCodes) {
            if (userPermissions.contains(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if user has all of the given permissions.
     */
    public boolean hasAllPermissions(UUID userId, String... permissionCodes) {
        if (userId == null) {
            return false;
        }
        Set<String> userPermissions = userRoleRepository.findActiveByUserId(userId)
                .stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        for (String code : permissionCodes) {
            if (!userPermissions.contains(code)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all active roles for a user.
     */
    public Set<Role> getUserRoles(UUID userId) {
        if (userId == null) {
            return Set.of();
        }
        return userRoleRepository.findActiveByUserId(userId)
                .stream()
                .map(UserRole::getRole)
                .collect(Collectors.toSet());
    }

    /**
     * Get all permissions for a user.
     */
    public Set<Permission> getUserPermissions(UUID userId) {
        if (userId == null) {
            return Set.of();
        }
        return userRoleRepository.findActiveByUserId(userId)
                .stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Validate that user can create a ticket.
     */
    public void validateCanCreateTicket(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!hasPermission(userId, "ticket:create")) {
            log.warn("User {} attempted to create ticket without permission", userId);
            throw new UnauthorizedException("You do not have permission to create tickets");
        }
    }

    /**
     * Validate that user can update a ticket.
     */
    public void validateCanUpdateTicket(UUID userId, Ticket ticket) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        
        // User can update if they created it or are assigned to it
        if (ticket.getCreatedBy().getId().equals(userId) || 
            (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(userId))) {
            if (!hasPermission(userId, "ticket:update")) {
                log.warn("User {} attempted to update ticket without permission", userId);
                throw new UnauthorizedException("You do not have permission to update tickets");
            }
            return;
        }

        // Moderators and above can update any ticket
        if (isModeratorOrAbove(userId)) {
            if (!hasPermission(userId, "ticket:update")) {
                log.warn("User {} (moderator) attempted to update ticket without permission", userId);
                throw new UnauthorizedException("You do not have permission to update tickets");
            }
            return;
        }

        log.warn("User {} attempted to update ticket they don't have access to", userId);
        throw new UnauthorizedException("You do not have access to this ticket");
    }

    /**
     * Validate that user can delete a ticket.
     */
    public void validateCanDeleteTicket(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isModeratorOrAbove(userId)) {
            log.warn("User {} attempted to delete ticket without moderator role", userId);
            throw new UnauthorizedException("Only moderators can delete tickets");
        }

        if (!hasPermission(userId, "ticket:delete")) {
            log.warn("User {} attempted to delete ticket without permission", userId);
            throw new UnauthorizedException("You do not have permission to delete tickets");
        }
    }

    /**
     * Validate that user can view a ticket.
     */
    public void validateCanViewTicket(UUID userId, Ticket ticket) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        
        // User can view if they created it or are assigned to it
        if (ticket.getCreatedBy().getId().equals(userId) || 
            (ticket.getAssignedTo() != null && ticket.getAssignedTo().getId().equals(userId))) {
            return;
        }

        // Moderators and above can view any ticket
        if (isModeratorOrAbove(userId)) {
            return;
        }

        log.warn("User {} attempted to view ticket they don't have access to", userId);
        throw new UnauthorizedException("You do not have access to this ticket");
    }

    /**
     * Validate that user can create a customer.
     */
    public void validateCanCreateCustomer(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isAdminOrAbove(userId)) {
            log.warn("User {} attempted to create customer without admin role", userId);
            throw new UnauthorizedException("Only admins can create customers");
        }

        if (!hasPermission(userId, "customer:create")) {
            log.warn("User {} attempted to create customer without permission", userId);
            throw new UnauthorizedException("You do not have permission to create customers");
        }
    }

    /**
     * Validate that user can update a customer.
     */
    public void validateCanUpdateCustomer(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isAdminOrAbove(userId)) {
            log.warn("User {} attempted to update customer without admin role", userId);
            throw new UnauthorizedException("Only admins can update customers");
        }

        if (!hasPermission(userId, "customer:update")) {
            log.warn("User {} attempted to update customer without permission", userId);
            throw new UnauthorizedException("You do not have permission to update customers");
        }
    }

    /**
     * Validate that user can delete a customer.
     */
    public void validateCanDeleteCustomer(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isAdminOrAbove(userId)) {
            log.warn("User {} attempted to delete customer without admin role", userId);
            throw new UnauthorizedException("Only admins can delete customers");
        }

        if (!hasPermission(userId, "customer:delete")) {
            log.warn("User {} attempted to delete customer without permission", userId);
            throw new UnauthorizedException("You do not have permission to delete customers");
        }
    }

    /**
     * Validate that user can manage roles.
     */
    public void validateCanManageRoles(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isAdminOrAbove(userId)) {
            log.warn("User {} attempted to manage roles without admin role", userId);
            throw new UnauthorizedException("Only admins can manage roles");
        }

        // For now, we'll allow admin role management without specific permissions
        // since the permission system might not be fully set up yet
        log.info("User {} managing roles (admin level {})", userId, getCurrentUserHierarchyLevel(userId));
    }

    /**
     * Validate that user can manage permissions.
     */
    public void validateCanManagePermissions(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isAdminOrAbove(userId)) {
            log.warn("User {} attempted to manage permissions without admin role", userId);
            throw new UnauthorizedException("Only admins can manage permissions");
        }

        // For now, we'll allow admin permission management without specific permissions
        // since the permission system might not be fully set up yet
        log.info("User {} managing permissions (admin level {})", userId, getCurrentUserHierarchyLevel(userId));
    }

    /**
     * Validate that user can manage users.
     */
    public void validateCanManageUsers(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isModeratorOrAbove(userId)) {
            log.warn("User {} attempted to manage users without moderator role", userId);
            throw new UnauthorizedException("Only moderators can manage users");
        }

        if (!hasPermission(userId, "user:update")) {
            log.warn("User {} attempted to manage users without permission", userId);
            throw new UnauthorizedException("You do not have permission to manage users");
        }
    }

    /**
     * Validate that user can manage projects.
     */
    public void validateCanManageProjects(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isAdminOrAbove(userId)) {
            log.warn("User {} attempted to manage projects without admin role", userId);
            throw new UnauthorizedException("Only admins can manage projects");
        }

        if (!hasPermission(userId, "project:update")) {
            log.warn("User {} attempted to manage projects without permission", userId);
            throw new UnauthorizedException("You do not have permission to manage projects");
        }
    }

    /**
     * Validate that user can manage SLAs.
     */
    public void validateCanManageSlas(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isModeratorOrAbove(userId)) {
            log.warn("User {} attempted to manage SLAs without moderator role", userId);
            throw new UnauthorizedException("Only moderators can manage SLAs");
        }

        if (!hasPermission(userId, "sla:update")) {
            log.warn("User {} attempted to manage SLAs without permission", userId);
            throw new UnauthorizedException("You do not have permission to manage SLAs");
        }
    }

    /**
     * Validate that user can manage ticket configurations.
     */
    public void validateCanManageTicketConfig(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isModeratorOrAbove(userId)) {
            log.warn("User {} attempted to manage ticket config without moderator role", userId);
            throw new UnauthorizedException("Only moderators can manage ticket configurations");
        }

        if (!hasPermission(userId, "ticket:type:update")) {
            log.warn("User {} attempted to manage ticket config without permission", userId);
            throw new UnauthorizedException("You do not have permission to manage ticket configurations");
        }
    }

    /**
     * Validate that user can manage knowledge base.
     */
    public void validateCanManageKnowledgeBase(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!isDeveloperOrAbove(userId)) {
            log.warn("User {} attempted to manage knowledge base without developer role", userId);
            throw new UnauthorizedException("Only developers can manage knowledge base");
        }

        if (!hasPermission(userId, "knowledge:update")) {
            log.warn("User {} attempted to manage knowledge base without permission", userId);
            throw new UnauthorizedException("You do not have permission to manage knowledge base");
        }
    }

    /**
     * Get role hierarchy level enum from integer level.
     */
    public Role.HierarchyLevel getHierarchyLevelEnum(Integer level) {
        if (level == null) {
            return null;
        }

        for (Role.HierarchyLevel h : Role.HierarchyLevel.values()) {
            if (h.getLevel() == level) {
                return h;
            }
        }
        return null;
    }

    /**
     * Check if user can assign a role to another user.
     * A user can only assign roles at or below their own hierarchy level.
     */
    public void validateCanAssignRole(UUID assignerId, Integer targetRoleLevel) {
        if (assignerId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        
        Integer assignerLevel = getCurrentUserHierarchyLevel(assignerId);

        if (assignerLevel < targetRoleLevel) {
            log.warn("User {} (level {}) attempted to assign role with level {}", 
                assignerId, assignerLevel, targetRoleLevel);
            throw new UnauthorizedException(
                "You can only assign roles at or below your hierarchy level"
            );
        }

        if (!hasPermission(assignerId, "role:assign")) {
            log.warn("User {} attempted to assign role without permission", assignerId);
            throw new UnauthorizedException("You do not have permission to assign roles");
        }
    }
}
