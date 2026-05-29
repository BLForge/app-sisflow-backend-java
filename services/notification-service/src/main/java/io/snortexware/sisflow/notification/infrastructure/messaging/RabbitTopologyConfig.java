package io.snortexware.sisflow.notification.infrastructure.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTopologyConfig {

    @Bean
    DirectExchange notificationsExchange(@Value("${notifications.exchange}") String exchange) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    Queue notificationEmailQueue(@Value("${notifications.email.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    Binding authEmailBinding(Queue notificationEmailQueue,
                             DirectExchange notificationsExchange,
                             @Value("${notifications.email.routing-key}") String routingKey) {
        return BindingBuilder.bind(notificationEmailQueue).to(notificationsExchange).with(routingKey);
    }
}
