package io.snortexware.sisflow.auth.infrastructure.messaging;

import io.snortexware.sisflow.auth.application.port.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitNotificationPublisher implements NotificationPort {

    private final RabbitTemplate rabbitTemplate;

    @Value("${notifications.exchange}")
    private String exchange;

    @Value("${notifications.email.routing-key}")
    private String routingKey;

    @Override
    public void publishConfirmationEmail(String recipient, String token) {
        rabbitTemplate.convertAndSend(exchange, routingKey,
                new AuthEmailNotificationMessage("CONFIRM_EMAIL", recipient, token));
    }

    @Override
    public void publishPasswordResetEmail(String recipient, String token) {
        rabbitTemplate.convertAndSend(exchange, routingKey,
                new AuthEmailNotificationMessage("RESET_PASSWORD", recipient, token));
    }

    @Configuration
    static class MessagingConfig {
        @Bean
        DirectExchange notificationsExchange(@Value("${notifications.exchange}") String exchangeName) {
            return new DirectExchange(exchangeName, true, false);
        }
    }
}
