package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateKnowledgeBaseRequest;
import io.snortexware.sisflow.dto.UpdateKnowledgeBaseRequest;
import io.snortexware.sisflow.entities.KnowledgeBase;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBaseController extends BaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final AuthorizationService authorizationService;

    @Override
    protected AuthorizationService authorizationService() { return authorizationService; }

    @GetMapping
    public ResponseEntity<List<KnowledgeBase>> list(@RequestParam(required = false) String q,
                                                    @AuthenticationPrincipal UUID callerId) {
        requireDeveloper(callerId);
        return ResponseEntity.ok(knowledgeBaseService.list(q));
    }

    @PostMapping
    public ResponseEntity<KnowledgeBase> create(@Valid @RequestBody CreateKnowledgeBaseRequest request,
                                                @AuthenticationPrincipal UUID callerId) {
        requireDeveloper(callerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(knowledgeBaseService.create(callerId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBase> update(@PathVariable UUID id,
                                                @Valid @RequestBody UpdateKnowledgeBaseRequest request,
                                                @AuthenticationPrincipal UUID callerId) {
        requireDeveloper(callerId);
        return ResponseEntity.ok(knowledgeBaseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        requireDeveloper(callerId);
        knowledgeBaseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
