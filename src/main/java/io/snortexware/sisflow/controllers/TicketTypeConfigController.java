package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateTicketTypeRequest;
import io.snortexware.sisflow.dto.UpdateTicketTypeRequest;
import io.snortexware.sisflow.entities.TicketTypeConfig;
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
@RequestMapping("/ticket-types")
public class TicketTypeConfigController {

    private final ParametricConfigService parametricConfigService;
    private final AuthorizationService authorizationService;

    public TicketTypeConfigController(ParametricConfigService parametricConfigService, AuthorizationService authorizationService) {
        this.parametricConfigService = parametricConfigService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<TicketTypeConfig>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can view ticket types
            if (authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.ok(parametricConfigService.listTypes());
            } else {
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping
    public ResponseEntity<TicketTypeConfig> create(@Valid @RequestBody CreateTicketTypeRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can create ticket types
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(parametricConfigService.createType(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketTypeConfig> update(@PathVariable UUID id,
                                                   @Valid @RequestBody UpdateTicketTypeRequest request,
                                                   @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can update ticket types
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(parametricConfigService.updateType(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can delete ticket types
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        parametricConfigService.deleteType(id);
        return ResponseEntity.noContent().build();
    }
}
