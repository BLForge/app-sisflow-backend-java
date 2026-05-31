package io.snortexware.sisflow.notifications;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitTicketNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${notifications.exchange}")
    private String exchange;

    @Value("${notifications.ticket.routing-key}")
    private String routingKey;

    public void publish(TicketNotificationMessage message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    @Configuration
    static class MessagingConfig {
        @Bean
        DirectExchange notificationsExchange(@Value("${notifications.exchange}") String exchangeName) {
            return new DirectExchange(exchangeName, true, false);
        }
    }
}
