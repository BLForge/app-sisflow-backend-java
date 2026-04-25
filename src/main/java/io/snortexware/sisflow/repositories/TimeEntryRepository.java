package io.snortexware.sisflow.repositories;

import io.snortexware.sisflow.entities.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {
    List<TimeEntry> findByTicketId(UUID ticketId);
    List<TimeEntry> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);

    @Query("SELECT COALESCE(SUM(t.hours), 0) FROM TimeEntry t WHERE t.ticket.id = :ticketId")
    BigDecimal sumHoursByTicketId(UUID ticketId);
}
