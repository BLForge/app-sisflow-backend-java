package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateTicketStatusRequest;
import io.snortexware.sisflow.dto.UpdateTicketStatusRequest;
import io.snortexware.sisflow.entities.TicketStatusConfig;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.ParametricConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ticket-statuses")
public class TicketStatusConfigController {

    private final ParametricConfigService parametricConfigService;
    private final AuthorizationService authorizationService;

    public TicketStatusConfigController(ParametricConfigService parametricConfigService, AuthorizationService authorizationService) {
        this.parametricConfigService = parametricConfigService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<TicketStatusConfig>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can view ticket statuses
            if (authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.ok(parametricConfigService.listStatuses());
            } else {
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping
    public ResponseEntity<TicketStatusConfig> create(@Valid @RequestBody CreateTicketStatusRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can create ticket statuses
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(parametricConfigService.createStatus(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketStatusConfig> update(@PathVariable UUID id,
                                                     @Valid @RequestBody UpdateTicketStatusRequest request,
                                                     @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can update ticket statuses
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(parametricConfigService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can delete ticket statuses
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        parametricConfigService.deleteStatus(id);
        return ResponseEntity.noContent().build();
    }
}
