package io.snortexware.sisflow.notification.infrastructure.messaging;

import io.snortexware.sisflow.notification.application.TicketNotificationHandler;
import io.snortexware.sisflow.notification.domain.model.TicketNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TicketNotificationListener {

    private final TicketNotificationHandler ticketNotificationHandler;

    @RabbitListener(queues = "${notifications.ticket.queue}")
    public void consume(TicketNotification notification) {
        ticketNotificationHandler.handle(notification);
    }
}
