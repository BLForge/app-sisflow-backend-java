package io.snortexware.sisflow.controllers;

import io.snortexware.sisflow.dto.LogTimeRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TimeEntry;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.security.exceptions.AppException;
import io.snortexware.sisflow.services.AuthorizationService;
import io.snortexware.sisflow.services.TimeEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tickets/{id}/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;
    private final TicketRepository ticketRepository;
    private final AuthorizationService authorizationService;

    @GetMapping
    public ResponseEntity<List<TimeEntry>> list(@PathVariable UUID id, @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        Ticket ticket = ticketRepository.findById(id).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        return ResponseEntity.ok(timeEntryService.list(id));
    }

    @PostMapping
    public ResponseEntity<TimeEntry> log(@PathVariable UUID id,
            @Valid @RequestBody LogTimeRequest request,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        Ticket ticket = ticketRepository.findById(id).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        return ResponseEntity.status(HttpStatus.CREATED).body(timeEntryService.log(id, callerId, request));
    }

    @DeleteMapping("/{entryId}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @PathVariable UUID entryId,
            @AuthenticationPrincipal UUID callerId) {
        if (callerId == null) throw AppException.unauthorized();
        Ticket ticket = ticketRepository.findById(id).orElseThrow(AppException::notFound);
        authorizationService.validateCanViewTicket(callerId, ticket);
        timeEntryService.delete(id, entryId);
        return ResponseEntity.noContent().build();
    }
}
