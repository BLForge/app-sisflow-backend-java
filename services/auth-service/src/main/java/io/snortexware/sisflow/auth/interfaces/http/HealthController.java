package io.snortexware.sisflow.auth.interfaces.http;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> checks = new LinkedHashMap<>();

        boolean databaseUp = checkDatabase();
        boolean redisUp = checkRedis();
        boolean rabbitUp = checkRabbit();

        checks.put("database", databaseUp ? "UP" : "DOWN");
        checks.put("redis", redisUp ? "UP" : "DOWN");
        checks.put("rabbitmq", rabbitUp ? "UP" : "DOWN");

        boolean healthy = databaseUp && redisUp && rabbitUp;

        return ResponseEntity.status(healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", healthy ? "UP" : "DOWN",
                        "checks", checks
                ));
    }

    private boolean checkDatabase() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean checkRedis() {
        try {
            String response = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
            return "PONG".equalsIgnoreCase(response);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean checkRabbit() {
        try {
            return Boolean.TRUE.equals(rabbitTemplate.execute(channel -> channel.isOpen()));
        } catch (Exception ignored) {
            return false;
        }
    }
}
