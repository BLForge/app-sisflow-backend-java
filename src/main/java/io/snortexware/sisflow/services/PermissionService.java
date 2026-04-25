package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.CreatePermissionRequest;
import io.snortexware.sisflow.dto.UpdatePermissionRequest;
import io.snortexware.sisflow.entities.Permission;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service interface for permission management and validation.
 */
public interface PermissionService {

    // Permission CRUD Operations
    Permission createPermission(CreatePermissionRequest request);
    Permission updatePermission(UUID permissionId, UpdatePermissionRequest request);
    void deletePermission(UUID permissionId);
    Permission getPermissionById(UUID permissionId);
    List<Permission> getAllPermissions();
    Permission getPermissionByCode(String code);

    // Role-Permission Mapping
    void grantPermissionToRole(UUID roleId, UUID permissionId);
    void revokePermissionFromRole(UUID roleId, UUID permissionId);
    Set<Permission> getRolePermissions(UUID roleId);

    // User Permission Checks
    boolean hasPermission(UUID userId, String permissionCode);
    boolean hasAnyPermission(UUID userId, List<String> permissionCodes);
    boolean hasAllPermissions(UUID userId, List<String> permissionCodes);
    Set<Permission> getUserPermissions(UUID userId);

    // Resource-Level Permissions
    boolean canAccessResource(UUID userId, UUID resourceId, String action);
    boolean canAccessTicket(UUID userId, UUID ticketId, String action);

    // Utility Methods
    List<Permission> getPermissionsByCategory(String category);
}
