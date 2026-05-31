package io.snortexware.sisflow.notification.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketNotification(
        TicketNotificationType type,
        UUID userId,
        UUID ticketId,
        Long ticketCode,
        String ticketTitle,
        String message,
        UUID transferredByUserId,
        String transferredByName,
        OffsetDateTime occurredAt
) {
}
