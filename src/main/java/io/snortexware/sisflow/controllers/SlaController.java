package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateSlaRequest;
import io.snortexware.sisflow.dto.UpdateSlaRequest;
import io.snortexware.sisflow.entities.Sla;
import io.snortexware.sisflow.repositories.SlaRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slas")
public class SlaController {

    private final SlaRepository slaRepository;
    private final AuthorizationService authorizationService;

    public SlaController(SlaRepository slaRepository, AuthorizationService authorizationService) {
        this.slaRepository = slaRepository;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    public ResponseEntity<Sla> create(@Valid @RequestBody CreateSlaRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can create SLAs
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Sla sla = Sla.builder()
                .name(request.getName())
                .responseTimeHours(request.getResponseTimeHours())
                .resolutionTimeHours(request.getResolutionTimeHours())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(slaRepository.save(sla));
    }

    @GetMapping
    public ResponseEntity<List<Sla>> list(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can view SLAs
            if (authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.ok(slaRepository.findAll());
            } else {
                // Non-moderators get empty list
                return ResponseEntity.ok(List.of());
            }
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sla> update(@PathVariable UUID id, @Valid @RequestBody UpdateSlaRequest request, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            // Only moderators and above can update SLAs
            if (!authorizationService.isModeratorOrAbove(callerId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        return slaRepository.findById(id)
                .map(sla -> {
                    sla.setName(request.getName());
                    sla.setResponseTimeHours(request.getResponseTimeHours());
                    sla.setResolutionTimeHours(request.getResolutionTimeHours());
                    return ResponseEntity.ok(slaRepository.save(sla));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
