package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.PermissionValidationRequest;
import io.snortexware.sisflow.dto.PermissionValidationResponse;
import io.snortexware.sisflow.services.PermissionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for permission validation endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionValidationController {

    private final PermissionService permissionService;

    public PermissionValidationController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Validate if a user has a specific permission.
     * POST /api/v1/permissions/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<PermissionValidationResponse> validatePermission(
            @Valid @RequestBody PermissionValidationRequest request) {
        log.info("Validating permission {} for user {}", request.getPermissionCode(), request.getUserId());

        boolean hasPermission = permissionService.hasPermission(request.getUserId(), request.getPermissionCode());

        PermissionValidationResponse response = PermissionValidationResponse.builder()
                .userId(request.getUserId())
                .permissionCode(request.getPermissionCode())
                .hasPermission(hasPermission)
                .build();

        return ResponseEntity.ok(response);
    }
}
