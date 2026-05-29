package io.snortexware.sisflow.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate stringRedisTemplate;

    public RateLimitResult consume(String key, long limit, Duration window) {
        long windowSeconds = Math.max(1, window.getSeconds());
        long windowStart = Instant.now().getEpochSecond() / windowSeconds;
        String namespacedKey = "rate-limit:" + key + ":" + windowStart;

        Long currentCount = stringRedisTemplate.opsForValue().increment(namespacedKey);
        if (currentCount == null) {
            currentCount = 0L;
        }

        if (currentCount == 1L) {
            stringRedisTemplate.expire(namespacedKey, window.plusSeconds(5));
        }

        long remaining = Math.max(0, limit - currentCount);
        return new RateLimitResult(currentCount <= limit, remaining, currentCount);
    }

    public void clearBucket(String key) {
        String prefix = "rate-limit:" + key + ":";
        var keys = stringRedisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    public record RateLimitResult(boolean allowed, long remaining, long currentCount) {}
}
