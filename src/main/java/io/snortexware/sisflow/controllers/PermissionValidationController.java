package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.PermissionValidationRequest;
import io.snortexware.sisflow.dto.PermissionValidationResponse;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionValidationController {

    private final PermissionService permissionService;
    private final AuthorizationService authorizationService;

    @PostMapping("/validate")
    public ResponseEntity<PermissionValidationResponse> validatePermission(
            @Valid @RequestBody PermissionValidationRequest request,
            @AuthenticationPrincipal UUID callerId) {

        if (callerId == null || !authorizationService.isModeratorOrAbove(callerId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");

        log.info("Validating permission {} for user {}", request.getPermissionCode(), request.getUserId());
        return ResponseEntity.ok(PermissionValidationResponse.builder()
                .userId(request.getUserId())
                .permissionCode(request.getPermissionCode())
                .hasPermission(permissionService.hasPermission(request.getUserId(), request.getPermissionCode()))
                .build());
    }
}
