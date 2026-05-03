package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateSlaRequest;
import io.snortexware.sisflow.dto.UpdateSlaRequest;
import io.snortexware.sisflow.entities.Sla;
import io.snortexware.sisflow.repositories.SlaRepository;
import io.snortexware.sisflow.services.AuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slas")
@RequiredArgsConstructor
public class SlaController extends BaseController {

    private final SlaRepository slaRepository;
    private final AuthorizationService authorizationService;

    @Override
    protected AuthorizationService authorizationService() { return authorizationService; }

    @GetMapping
    public ResponseEntity<List<Sla>> list(@AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(slaRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<Sla> create(@Valid @RequestBody CreateSlaRequest request,
                                      @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(slaRepository.save(
                Sla.builder()
                        .name(request.getName())
                        .responseTimeHours(request.getResponseTimeHours())
                        .resolutionTimeHours(request.getResolutionTimeHours())
                        .build()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Sla> update(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateSlaRequest request,
                                      @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        Sla sla = slaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SLA not found"));
        sla.setName(request.getName());
        sla.setResponseTimeHours(request.getResponseTimeHours());
        sla.setResolutionTimeHours(request.getResolutionTimeHours());
        return ResponseEntity.ok(slaRepository.save(sla));
    }
}
