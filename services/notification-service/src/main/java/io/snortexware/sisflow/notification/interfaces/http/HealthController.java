package io.snortexware.sisflow.notification.interfaces.http;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final RabbitTemplate rabbitTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean rabbitUp = checkRabbit();
        return ResponseEntity.status(rabbitUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", rabbitUp ? "UP" : "DOWN",
                        "checks", Map.of("rabbitmq", rabbitUp ? "UP" : "DOWN")
                ));
    }

    private boolean checkRabbit() {
        try {
            return Boolean.TRUE.equals(rabbitTemplate.execute(channel -> channel.isOpen()));
        } catch (Exception ignored) {
            return false;
        }
    }
}
