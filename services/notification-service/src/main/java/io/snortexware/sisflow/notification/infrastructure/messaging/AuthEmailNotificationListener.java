package io.snortexware.sisflow.notification.infrastructure.messaging;

import io.snortexware.sisflow.notification.application.EmailNotificationHandler;
import io.snortexware.sisflow.notification.domain.model.AuthEmailNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthEmailNotificationListener {

    private final EmailNotificationHandler emailNotificationHandler;

    @RabbitListener(queues = "${notifications.email.queue}")
    public void consume(AuthEmailNotification notification) {
        emailNotificationHandler.handle(notification);
    }
}
