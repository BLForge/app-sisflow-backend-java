package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreatePermissionRequest;
import io.snortexware.sisflow.dto.UpdatePermissionRequest;
import io.snortexware.sisflow.entities.Permission;
import io.snortexware.sisflow.entities.ResourcePermission;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.PermissionRepository;
import io.snortexware.sisflow.repositories.ResourcePermissionRepository;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.security.exceptions.AccessDeniedException;
import io.snortexware.sisflow.security.exceptions.InvalidPermissionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of PermissionService for permission management and validation.
 */
@Slf4j
@Service
@Transactional
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ResourcePermissionRepository resourcePermissionRepository;
    private final AuditService auditService;

    public PermissionServiceImpl(
            PermissionRepository permissionRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            ResourcePermissionRepository resourcePermissionRepository,
            AuditService auditService) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.resourcePermissionRepository = resourcePermissionRepository;
        this.auditService = auditService;
    }

    @Override
    @CacheEvict(value = "permissions", allEntries = true)
    public Permission createPermission(CreatePermissionRequest request) {
        log.info("Creating new permission: {}", request.getCode());

        // Validate permission code is unique
        if (permissionRepository.findByCode(request.getCode()).isPresent()) {
            throw new InvalidPermissionException("Permission with code '" + request.getCode() + "' already exists");
        }

        // Validate permission code format (resource:action)
        if (!request.getCode().matches("^[a-z_]+:[a-z_]+$")) {
            throw new InvalidPermissionException(
                    "Invalid permission code format. Must follow pattern: resource:action (e.g., ticket:create)");
        }

        Permission permission = Permission.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .isSystem(false)
                .isActive(true)
                .build();

        Permission savedPermission = permissionRepository.save(permission);
        log.info("Permission created successfully: {} (id={})", savedPermission.getCode(), savedPermission.getId());

        return savedPermission;
    }

    @Override
    @CacheEvict(value = "permissions", allEntries = true)
    public Permission updatePermission(UUID permissionId, UpdatePermissionRequest request) {
        log.info("Updating permission: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new InvalidPermissionException("Permission not found: " + permissionId));

        // Prevent modification of system permissions
        if (permission.getIsSystem()) {
            throw new AccessDeniedException("Cannot modify system permissions");
        }

        if (request.getName() != null) {
            permission.setName(request.getName());
        }
        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }

        Permission updatedPermission = permissionRepository.save(permission);
        log.info("Permission updated successfully: {}", permissionId);

        return updatedPermission;
    }

    @Override
    @CacheEvict(value = "permissions", allEntries = true)
    public void deletePermission(UUID permissionId) {
        log.info("Deleting permission: {}", permissionId);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new InvalidPermissionException("Permission not found: " + permissionId));

        // Prevent deletion of system permissions
        if (permission.getIsSystem()) {
            throw new AccessDeniedException("Cannot delete system permissions");
        }

        // Check if permission is assigned to roles with active users
        List<Role> rolesWithPermission = roleRepository.findAll().stream()
                .filter(role -> role.getPermissions().contains(permission))
                .collect(Collectors.toList());

        for (Role role : rolesWithPermission) {
            List<UserRole> activeUserRoles = userRoleRepository.findActiveByRoleId(role.getId());
            if (!activeUserRoles.isEmpty()) {
                throw new AccessDeniedException(
                        "Cannot delete permission assigned to roles with active users. " +
                        "Permission is assigned to role '" + role.getCode() + "' with " +
                        activeUserRoles.size() + " active users.");
            }
        }

        permissionRepository.deleteById(permissionId);
        log.info("Permission deleted successfully: {}", permissionId);
    }

    @Override
    @Cacheable(value = "permissions", key = "#permissionId")
    public Permission getPermissionById(UUID permissionId) {
        return permissionRepository.findById(permissionId)
                .orElseThrow(() -> new InvalidPermissionException("Permission not found: " + permissionId));
    }

    @Override
    @Cacheable(value = "permissions", key = "'all'")
    public List<Permission> getAllPermissions() {
        return permissionRepository.findByIsActiveTrue();
    }

    @Override
    public Permission getPermissionByCode(String code) {
        return permissionRepository.findByCode(code)
                .orElseThrow(() -> new InvalidPermissionException("Permission not found: " + code));
    }

    @Override
    @CacheEvict(value = {"permissions", "userPermissions"}, allEntries = true)
    public void grantPermissionToRole(UUID roleId, UUID permissionId) {
        log.info("Granting permission {} to role {}", permissionId, roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new InvalidPermissionException("Role not found: " + roleId));

        Permission permission = getPermissionById(permissionId);

        // Check if permission is already assigned
        if (role.getPermissions().contains(permission)) {
            throw new InvalidPermissionException("Permission already assigned to this role");
        }

        role.getPermissions().add(permission);
        roleRepository.save(role);

        log.info("Permission granted successfully: role={}, permission={}", roleId, permissionId);
        auditService.logPermissionChange(roleId, permissionId, "GRANT");
    }

    @Override
    @CacheEvict(value = {"permissions", "userPermissions"}, allEntries = true)
    public void revokePermissionFromRole(UUID roleId, UUID permissionId) {
        log.info("Revoking permission {} from role {}", permissionId, roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new InvalidPermissionException("Role not found: " + roleId));

        Permission permission = getPermissionById(permissionId);

        // Check if permission is assigned
        if (!role.getPermissions().contains(permission)) {
            throw new InvalidPermissionException("Permission not assigned to this role");
        }

        role.getPermissions().remove(permission);
        roleRepository.save(role);

        log.info("Permission revoked successfully: role={}, permission={}", roleId, permissionId);
        auditService.logPermissionChange(roleId, permissionId, "REVOKE");
    }

    @Override
    @Cacheable(value = "rolePermissions", key = "#roleId")
    public Set<Permission> getRolePermissions(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new InvalidPermissionException("Role not found: " + roleId));
        return new HashSet<>(role.getPermissions());
    }

    @Override
    @Cacheable(value = "userPermissions", key = "#userId")
    public boolean hasPermission(UUID userId, String permissionCode) {
        Set<Permission> userPermissions = getUserPermissions(userId);
        return userPermissions.stream()
                .anyMatch(p -> p.getCode().equals(permissionCode));
    }

    @Override
    public boolean hasAnyPermission(UUID userId, List<String> permissionCodes) {
        Set<Permission> userPermissions = getUserPermissions(userId);
        return userPermissions.stream()
                .anyMatch(p -> permissionCodes.contains(p.getCode()));
    }

    @Override
    public boolean hasAllPermissions(UUID userId, List<String> permissionCodes) {
        Set<Permission> userPermissions = getUserPermissions(userId);
        Set<String> userPermissionCodes = userPermissions.stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());
        return userPermissionCodes.containsAll(permissionCodes);
    }

    @Override
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<Permission> getUserPermissions(UUID userId) {
        Set<Permission> permissions = new HashSet<>();

        // Get all active roles for the user
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);

        // Collect all permissions from all roles
        for (UserRole userRole : userRoles) {
            permissions.addAll(userRole.getRole().getPermissions());
        }

        // Add resource-level permissions
        List<ResourcePermission> resourcePermissions = resourcePermissionRepository.findByUserId(userId);
        for (ResourcePermission rp : resourcePermissions) {
            if (!rp.isExpired() && rp.getIsActive()) {
                // Create a virtual permission for resource-level access
                Permission permission = Permission.builder()
                        .code(rp.getResourceType() + ":" + rp.getAction().toLowerCase())
                        .name(rp.getResourceType() + " " + rp.getAction())
                        .category("RESOURCE")
                        .isSystem(false)
                        .isActive(true)
                        .build();
                permissions.add(permission);
            }
        }

        return permissions;
    }

    @Override
    public boolean canAccessResource(UUID userId, UUID resourceId, String action) {
        // Check resource-level permissions
        List<ResourcePermission> resourcePermissions = resourcePermissionRepository
                .findActiveByResourceIdAndUserId(resourceId, userId, OffsetDateTime.now());

        for (ResourcePermission rp : resourcePermissions) {
            if (rp.getAction().equals(action) && !rp.isExpired() && rp.getIsActive()) {
                return true;
            }
        }

        // Check role-based permissions
        String permissionCode = "resource:" + action.toLowerCase();
        return hasPermission(userId, permissionCode);
    }

    @Override
    public boolean canAccessTicket(UUID userId, UUID ticketId, String action) {
        // Check resource-level permissions for ticket
        List<ResourcePermission> ticketPermissions = resourcePermissionRepository
                .findActiveByResourceIdAndUserId(ticketId, userId, OffsetDateTime.now());

        for (ResourcePermission rp : ticketPermissions) {
            if (rp.getAction().equals(action) && !rp.isExpired() && rp.getIsActive()) {
                return true;
            }
        }

        // Check role-based permissions
        String permissionCode = "ticket:" + action.toLowerCase();
        return hasPermission(userId, permissionCode);
    }

    @Override
    public List<Permission> getPermissionsByCategory(String category) {
        return permissionRepository.findByCategory(category);
    }
}
