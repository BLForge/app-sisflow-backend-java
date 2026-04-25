package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateRoleRequest;
import io.snortexware.sisflow.dto.UpdateRoleRequest;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.entities.UserRole;
import io.snortexware.sisflow.repositories.RoleRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import io.snortexware.sisflow.repositories.UserRoleRepository;
import io.snortexware.sisflow.security.TenantContext;
import io.snortexware.sisflow.security.exceptions.AccessDeniedException;
import io.snortexware.sisflow.security.exceptions.InvalidRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of RoleService for role management and hierarchy validation.
 */
@Slf4j
@Service
@Transactional
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserProfileRepository userProfileRepository;
    private final TenantContext tenantContext;
    private final AuditService auditService;

    public RoleServiceImpl(
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            UserProfileRepository userProfileRepository,
            TenantContext tenantContext,
            AuditService auditService) {
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.userProfileRepository = userProfileRepository;
        this.tenantContext = tenantContext;
        this.auditService = auditService;
    }

    @Override
    @CacheEvict(value = "roles", allEntries = true)
    public Role createRole(CreateRoleRequest request) {
        log.info("Creating new role: {}", request.getCode());

        // Validate role code is unique
        if (roleRepository.findByCode(request.getCode()).isPresent()) {
            throw new InvalidRoleException("Role with code '" + request.getCode() + "' already exists");
        }

        // Validate hierarchy level
        if (request.getHierarchyLevel() == null || request.getHierarchyLevel() < 0 || request.getHierarchyLevel() > 4) {
            throw new InvalidRoleException("Invalid hierarchy level. Must be between 0 and 4");
        }

        Role role = Role.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .hierarchyLevel(request.getHierarchyLevel())
                .isSystem(false)
                .isActive(true)
                .permissions(new HashSet<>())
                .build();

        Role savedRole = roleRepository.save(role);
        log.info("Role created successfully: {} (id={})", savedRole.getCode(), savedRole.getId());

        return savedRole;
    }

    @Override
    @CacheEvict(value = "roles", allEntries = true)
    public Role updateRole(UUID roleId, UpdateRoleRequest request) {
        log.info("Updating role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new InvalidRoleException("Role not found: " + roleId));

        // Prevent modification of system roles
        if (role.getIsSystem()) {
            throw new AccessDeniedException("Cannot modify system roles");
        }

        // Hierarchy level cannot be changed
        if (request.getHierarchyLevel() != null && !request.getHierarchyLevel().equals(role.getHierarchyLevel())) {
            throw new InvalidRoleException("Hierarchy level cannot be changed after role creation");
        }

        if (request.getName() != null) {
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        Role updatedRole = roleRepository.save(role);
        log.info("Role updated successfully: {}", roleId);

        return updatedRole;
    }

    @Override
    @CacheEvict(value = "roles", allEntries = true)
    public void deleteRole(UUID roleId) {
        log.info("Deleting role: {}", roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new InvalidRoleException("Role not found: " + roleId));

        // Prevent deletion of system roles
        if (role.getIsSystem()) {
            throw new AccessDeniedException("Cannot delete system roles");
        }

        // Check if role has active users
        List<UserRole> activeUserRoles = userRoleRepository.findActiveByRoleId(roleId);
        if (!activeUserRoles.isEmpty()) {
            throw new AccessDeniedException(
                    "Cannot delete role with " + activeUserRoles.size() + " active users. " +
                    "Please reassign users to another role first.");
        }

        roleRepository.deleteById(roleId);
        log.info("Role deleted successfully: {}", roleId);
    }

    @Override
    @Cacheable(value = "roles", key = "#roleId")
    public Role getRoleById(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new InvalidRoleException("Role not found: " + roleId));
    }

    @Override
    @Cacheable(value = "roles", key = "'all'")
    public List<Role> getAllRoles() {
        return roleRepository.findByIsActiveTrue();
    }

    @Override
    public Role getRoleByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new InvalidRoleException("Role not found: " + code));
    }

    @Override
    @CacheEvict(value = {"roles", "userPermissions"}, allEntries = true)
    public void assignRoleToUser(UUID userId, UUID roleId) {
        log.info("Assigning role {} to user {}", roleId, userId);

        UserProfile user = userProfileRepository.findById(userId)
                .orElseThrow(() -> new InvalidRoleException("User not found: " + userId));

        Role role = getRoleById(roleId);

        // Check if user already has this role
        if (userRoleRepository.findByUserIdAndRoleId(userId, roleId).isPresent()) {
            throw new InvalidRoleException("User already has this role");
        }

        // Validate hierarchy: user cannot have two roles of the same level
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        for (UserRole userRole : userRoles) {
            if (userRole.getRole().getHierarchyLevel().equals(role.getHierarchyLevel())) {
                throw new InvalidRoleException(
                        "User cannot have two roles of the same hierarchy level. " +
                        "Current role: " + userRole.getRole().getCode() + 
                        ", New role: " + role.getCode());
            }
        }

        UUID currentUserId = tenantContext.getCurrentUser();
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .isActive(true)
                .assignedAt(OffsetDateTime.now())
                .assignedBy(currentUserId)
                .build();

        userRoleRepository.save(userRole);
        log.info("Role assigned successfully: user={}, role={}", userId, roleId);

        // Audit log
        auditService.logRoleAssignment(userId, roleId, currentUserId);
    }

    @Override
    @CacheEvict(value = {"roles", "userPermissions"}, allEntries = true)
    public void removeRoleFromUser(UUID userId, UUID roleId) {
        log.info("Removing role {} from user {}", roleId, userId);

        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new InvalidRoleException("User does not have this role"));

        UUID currentUserId = tenantContext.getCurrentUser();
        userRole.setIsActive(false);
        userRole.setRevokedAt(OffsetDateTime.now());
        userRole.setRevokedBy(currentUserId);

        userRoleRepository.save(userRole);
        log.info("Role removed successfully: user={}, role={}", userId, roleId);

        // Audit log
        auditService.logRoleRevocation(userId, roleId, currentUserId);
    }

    @Override
    @Cacheable(value = "userRoles", key = "#userId")
    public List<Role> getUserRoles(UUID userId) {
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        return userRoles.stream()
                .map(UserRole::getRole)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Role> getUserRolesAsSet(UUID userId) {
        return new HashSet<>(getUserRoles(userId));
    }

    @Override
    public boolean canAssignRole(UUID assignerId, UUID targetRoleId) {
        try {
            UserProfile assigner = userProfileRepository.findById(assignerId)
                    .orElseThrow(() -> new InvalidRoleException("Assigner not found"));

            Role targetRole = getRoleById(targetRoleId);

            // Get assigner's highest role level
            List<UserRole> assignerRoles = userRoleRepository.findActiveByUserId(assignerId);
            if (assignerRoles.isEmpty()) {
                return false;
            }

            Integer assignerMaxLevel = assignerRoles.stream()
                    .map(ur -> ur.getRole().getHierarchyLevel())
                    .max(Integer::compareTo)
                    .orElse(-1);

            // Assigner can only assign roles of lower or equal level
            return assignerMaxLevel >= targetRole.getHierarchyLevel();
        } catch (Exception e) {
            log.warn("Error checking if user can assign role: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean canModifyRole(UUID userId, UUID roleId) {
        try {
            Role role = getRoleById(roleId);

            // System roles can only be modified by system admins
            if (role.getIsSystem()) {
                List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
                return userRoles.stream()
                        .anyMatch(ur -> ur.getRole().getHierarchyLevel() >= 4);
            }

            // Non-system roles can be modified by users with higher hierarchy level
            return canAssignRole(userId, roleId);
        } catch (Exception e) {
            log.warn("Error checking if user can modify role: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Integer getRoleLevel(Role role) {
        return role.getHierarchyLevel();
    }

    @Override
    public Integer getRoleLevelByCode(String roleCode) {
        Role role = getRoleByCode(roleCode);
        return role.getHierarchyLevel();
    }

    @Override
    public boolean isHigherOrEqual(Integer level1, Integer level2) {
        return level1 != null && level2 != null && level1 >= level2;
    }

    @Override
    @CacheEvict(value = {"roles", "userPermissions"}, allEntries = true)
    public void bulkAssignRoles(List<UUID> userIds, UUID roleId) {
        log.info("Bulk assigning role {} to {} users", roleId, userIds.size());

        Role role = getRoleById(roleId);
        UUID currentUserId = tenantContext.getCurrentUser();

        for (UUID userId : userIds) {
            try {
                // Skip if user already has this role
                if (userRoleRepository.findByUserIdAndRoleId(userId, roleId).isPresent()) {
                    log.debug("User {} already has role {}, skipping", userId, roleId);
                    continue;
                }

                assignRoleToUser(userId, roleId);
            } catch (Exception e) {
                log.warn("Failed to assign role to user {}: {}", userId, e.getMessage());
            }
        }

        log.info("Bulk role assignment completed for role {}", roleId);
    }

    @Override
    public List<UserRole> getUserRoleEntities(UUID userId) {
        return userRoleRepository.findActiveByUserId(userId);
    }

    @Override
    public boolean userHasRole(UUID userId, String roleCode) {
        return userRoleRepository.userHasRole(userId, roleCode);
    }

    @Override
    public List<Role> getRolesByHierarchyLevel(Integer level) {
        return roleRepository.findByHierarchyLevelGreaterThanOrEqual(level);
    }
}
