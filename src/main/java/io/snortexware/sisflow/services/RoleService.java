package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreateRoleRequest;
import io.snortexware.sisflow.dto.UpdateRoleRequest;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.entities.UserRole;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for role management and hierarchy validation.
 */
public interface RoleService {

    // Role CRUD Operations
    Role createRole(CreateRoleRequest request);
    Role updateRole(UUID roleId, UpdateRoleRequest request);
    void deleteRole(UUID roleId);
    Role getRoleById(UUID roleId);
    List<Role> getAllRoles();
    Role getRoleByCode(String code);

    // Role Assignment
    void assignRoleToUser(UUID userId, UUID roleId);
    void removeRoleFromUser(UUID userId, UUID roleId);
    List<Role> getUserRoles(UUID userId);
    Set<Role> getUserRolesAsSet(UUID userId);

    // Hierarchy Validation
    boolean canAssignRole(UUID assignerId, UUID targetRoleId);
    boolean canModifyRole(UUID userId, UUID roleId);
    Integer getRoleLevel(Role role);
    Integer getRoleLevelByCode(String roleCode);
    boolean isHigherOrEqual(Integer level1, Integer level2);

    // Bulk Operations
    void bulkAssignRoles(List<UUID> userIds, UUID roleId);

    // Utility Methods
    List<UserRole> getUserRoleEntities(UUID userId);
    boolean userHasRole(UUID userId, String roleCode);
    List<Role> getRolesByHierarchyLevel(Integer level);
}
