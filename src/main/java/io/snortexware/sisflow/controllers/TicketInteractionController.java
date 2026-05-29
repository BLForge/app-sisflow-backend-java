package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateInteractionRequest;
import io.snortexware.sisflow.entities.TicketInteraction;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.TicketInteractionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{id}/interactions")
@RequiredArgsConstructor
public class TicketInteractionController {

    private final TicketInteractionService ticketInteractionService;

    @GetMapping
    public ResponseEntity<List<TicketInteraction>> list(@PathVariable UUID id,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(ticketInteractionService.listForCaller(id, callerId));
    }

    @PostMapping
    public ResponseEntity<TicketInteraction> post(@PathVariable UUID id,
            @Valid @RequestBody CreateInteractionRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketInteractionService.postForCaller(id, callerId, request));
    }
}
