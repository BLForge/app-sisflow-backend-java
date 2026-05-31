package io.snortexware.sisflow.notifications;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketNotificationMessage(
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
