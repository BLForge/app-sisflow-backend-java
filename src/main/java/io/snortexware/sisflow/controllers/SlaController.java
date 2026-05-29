package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateSlaRequest;
import io.snortexware.sisflow.dto.UpdateSlaRequest;
import io.snortexware.sisflow.entities.Sla;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.SlaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slas")
@RequiredArgsConstructor
public class SlaController extends BaseController {

    private final SlaService slaService;
    private final AuthorizationService authorizationService;

    @Override
    protected AuthorizationService authorizationService() { return authorizationService; }

    @GetMapping
    @Cacheable(value = "slas", key = "@cacheKeyService.tenantKey('all')")
    public ResponseEntity<List<Sla>> list(@AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(slaService.list());
    }

    @PostMapping
    @CacheEvict(value = "slas", allEntries = true)
    public ResponseEntity<Sla> create(@Valid @RequestBody CreateSlaRequest request,
                                      @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(slaService.create(request));
    }

    @PutMapping("/{id}")
    @CacheEvict(value = "slas", allEntries = true)
    public ResponseEntity<Sla> update(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateSlaRequest request,
                                      @AuthenticationPrincipal UUID callerId) {
        requireModerator(callerId);
        return ResponseEntity.ok(slaService.update(id, request));
    }
}
