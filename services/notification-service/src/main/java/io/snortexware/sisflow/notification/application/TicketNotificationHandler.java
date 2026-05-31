package io.snortexware.sisflow.notification.application;

import io.snortexware.sisflow.notification.domain.model.TicketNotification;
import io.snortexware.sisflow.notification.infrastructure.sse.NotificationPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketNotificationHandler {

    private final NotificationPublisher notificationPublisher;

    public void handle(TicketNotification notification) {
        notificationPublisher.sendNotification(notification.userId(), notification);
    }
}
