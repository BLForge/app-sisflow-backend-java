package io.snortexware.sisflow.services;

import io.snortexware.sisflow.dto.LogTimeRequest;
import io.snortexware.sisflow.entities.Ticket;
import io.snortexware.sisflow.entities.TimeEntry;
import io.snortexware.sisflow.entities.UserProfile;
import io.snortexware.sisflow.repositories.TicketRepository;
import io.snortexware.sisflow.repositories.TimeEntryRepository;
import io.snortexware.sisflow.repositories.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final TicketRepository ticketRepository;
    private final UserProfileRepository userProfileRepository;
    private final AuditService auditService;

    @Transactional
    public TimeEntry log(UUID ticketId, UUID callerId, LogTimeRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));

        UserProfile user = userProfileRepository.findById(callerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User profile not found"));

        TimeEntry entry = TimeEntry.builder()
                .ticket(ticket)
                .user(user)
                .hours(request.getHours())
                .description(request.getDescription())
                .createdAt(OffsetDateTime.now())
                .build();

        TimeEntry saved = timeEntryRepository.save(entry);

        auditService.record(ticketId, callerId, AuditService.TIME_LOGGED, null, request.getHours().toString());

        return saved;
    }

    @Transactional
    public void delete(UUID ticketId, UUID entryId) {
        TimeEntry entry = timeEntryRepository.findById(entryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found"));

        if (!entry.getTicket().getId().equals(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Time entry not found for this ticket");
        }

        timeEntryRepository.delete(entry);
    }

    public List<TimeEntry> list(UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
        }
        return timeEntryRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }
}
