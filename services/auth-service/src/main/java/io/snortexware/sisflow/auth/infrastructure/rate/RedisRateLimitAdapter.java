package io.snortexware.sisflow.auth.infrastructure.rate;

import io.snortexware.sisflow.auth.application.port.RateLimitPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class RedisRateLimitAdapter implements RateLimitPort {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryConsume(String key, long limit, Duration window) {
        long windowSeconds = Math.max(1, window.getSeconds());
        long windowStart = Instant.now().getEpochSecond() / windowSeconds;
        String namespacedKey = "auth-rate-limit:" + key + ":" + windowStart;

        Long currentCount = stringRedisTemplate.opsForValue().increment(namespacedKey);
        if (currentCount == null) {
            return false;
        }
        if (currentCount == 1L) {
            stringRedisTemplate.expire(namespacedKey, window.plusSeconds(5));
        }
        return currentCount <= limit;
    }

    @Override
    public void clear(String key) {
        var keys = stringRedisTemplate.keys("auth-rate-limit:" + key + ":*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }
}
