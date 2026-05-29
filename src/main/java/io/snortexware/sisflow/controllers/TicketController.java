package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.CreateTicketRequest;
import io.snortexware.sisflow.dto.TransferTicketRequest;
import io.snortexware.sisflow.dto.UpdateTicketRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/check-code")
    public ResponseEntity<Boolean> checkCode(@RequestParam Long code,
            @RequestParam(required = false) UUID excludeId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(ticketService.isCodeAvailable(code, excludeId));
    }

    @PostMapping
    public ResponseEntity<Ticket> create(@Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal UUID callerId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(request, callerId));
    }

    @GetMapping
    public ResponseEntity<List<Ticket>> getTickets(@AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(ticketService.getTickets(callerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        return ResponseEntity.ok(ticketService.getTicketById(id, callerId));
    }

    @GetMapping("/my-queue")
    public ResponseEntity<List<Ticket>> getMyQueue(@AuthenticationPrincipal UUID callerId) {
        return ResponseEntity.ok(ticketService.getMyQueue(callerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Ticket> update(@PathVariable UUID id,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UUID callerId) {
        return ResponseEntity.ok(ticketService.updateTicket(id, request, callerId));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<Ticket> transfer(@PathVariable UUID id,
            @Valid @RequestBody TransferTicketRequest request,
            @AuthenticationPrincipal UUID callerId) {
        return ResponseEntity.ok(ticketService.transfer(id, callerId, request));
    }
}
