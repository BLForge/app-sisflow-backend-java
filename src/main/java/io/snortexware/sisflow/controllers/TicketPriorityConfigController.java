package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateTicketPriorityRequest;
import io.snortexware.sisflow.dto.UpdateTicketPriorityRequest;
import io.snortexware.sisflow.entities.TicketPriorityConfig;
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
@RequestMapping("/ticket-priorities")
public class TicketPriorityConfigController {

    private final ParametricConfigService parametricConfigService;
    private final AuthorizationService authorizationService;

    public TicketPriorityConfigController(ParametricConfigService parametricConfigService, AuthorizationService authorizationService) {
        this.parametricConfigService = parametricConfigService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<List<TicketPriorityConfig>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can view ticket priorities
            if (authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.ok(parametricConfigService.listPriorities());
            } else {
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PostMapping
    public ResponseEntity<TicketPriorityConfig> create(@Valid @RequestBody CreateTicketPriorityRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can create ticket priorities
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.status(HttpStatus.CREATED).body(parametricConfigService.createPriority(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketPriorityConfig> update(@PathVariable UUID id,
                                                       @Valid @RequestBody UpdateTicketPriorityRequest request,
                                                       @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can update ticket priorities
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return ResponseEntity.ok(parametricConfigService.updatePriority(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can delete ticket priorities
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        parametricConfigService.deletePriority(id);
        return ResponseEntity.noContent().build();
    }
}
