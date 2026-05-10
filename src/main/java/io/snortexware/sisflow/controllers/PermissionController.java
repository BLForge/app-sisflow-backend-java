package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreatePermissionRequest;
import io.snortexware.sisflow.dto.UpdatePermissionRequest;
import io.snortexware.sisflow.entities.Permission;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.PermissionService;
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
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;
    private final AuthorizationService authorizationService;

    @PostMapping
    @Transactional
    public ResponseEntity<Permission> createPermission(@Valid @RequestBody CreatePermissionRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManagePermissions(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.createPermission(request));
    }

    @PutMapping("/{permissionId}")
    @Transactional
    public ResponseEntity<Permission> updatePermission(@PathVariable UUID permissionId,
            @Valid @RequestBody UpdatePermissionRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManagePermissions(callerId);
        return ResponseEntity.ok(permissionService.updatePermission(permissionId, request));
    }

    @DeleteMapping("/{permissionId}")
    @Transactional
    public ResponseEntity<Void> deletePermission(@PathVariable UUID permissionId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManagePermissions(callerId);
        permissionService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{permissionId}")
    public ResponseEntity<Permission> getPermissionById(@PathVariable UUID permissionId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManagePermissions(callerId);
        return ResponseEntity.ok(permissionService.getPermissionById(permissionId));
    }

    @GetMapping
    public ResponseEntity<List<Permission>> getAllPermissions(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManagePermissions(callerId);
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Permission>> getPermissionsByCategory(@PathVariable String category,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        authorizationService.validateCanManagePermissions(callerId);
        return ResponseEntity.ok(permissionService.getPermissionsByCategory(category));
    }
}
