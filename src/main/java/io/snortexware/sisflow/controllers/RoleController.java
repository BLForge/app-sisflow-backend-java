package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateRoleRequest;
import io.snortexware.sisflow.dto.UpdateRoleRequest;
import io.snortexware.sisflow.entities.Role;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Transactional
    public ResponseEntity<Role> createRole(@Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageRoles(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }

    @PutMapping("/{roleId}")
    @Transactional
    public ResponseEntity<Role> updateRole(@PathVariable UUID roleId,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageRoles(callerId);
        return ResponseEntity.ok(roleService.updateRole(roleId, request));
    }

    @DeleteMapping("/{roleId}")
    @Transactional
    public ResponseEntity<Void> deleteRole(@PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageRoles(callerId);
        roleService.deleteRole(roleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<Role> getRoleById(@PathVariable UUID roleId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageRoles(callerId);
        return ResponseEntity.ok(roleService.getRoleById(roleId));
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageRoles(callerId);
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @GetMapping("/code/{code}")
    public ResponseEntity<Role> getRoleByCode(@PathVariable String code,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManageRoles(callerId);
        return ResponseEntity.ok(roleService.getRoleByCode(code));
    }
}
