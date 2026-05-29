package io.snortexware.sisflow.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisAdminService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisConnectionFactory redisConnectionFactory;

    public Map<String, String> health() {
        String response = stringRedisTemplate.execute((RedisCallback<String>) connection -> connection.ping());
        return Map.of("status", response == null ? "DOWN" : response);
    }

    public List<String> listKeys(String pattern) {
        String resolvedPattern = (pattern == null || pattern.isBlank()) ? "*" : pattern;
        Set<String> keys = stringRedisTemplate.keys(resolvedPattern);
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        return keys.stream().sorted().toList();
    }

    public Map<String, Object> getValue(String key) {
        String type = getType(key);
        Object value = switch (type) {
            case "string" -> stringRedisTemplate.opsForValue().get(key);
            case "hash" -> stringRedisTemplate.opsForHash().entries(key);
            case "list" -> stringRedisTemplate.opsForList().range(key, 0, -1);
            case "set" -> stringRedisTemplate.opsForSet().members(key);
            case "zset" -> stringRedisTemplate.opsForZSet().rangeWithScores(key, 0, -1);
            default -> readRawValue(key);
        };

        Long ttlSeconds = stringRedisTemplate.getExpire(key);

        return Map.of(
                "key", key,
                "type", type,
                "ttlSeconds", ttlSeconds == null ? -1 : ttlSeconds,
                "value", value == null ? "" : value
        );
    }

    public boolean exists(String key) {
        Boolean exists = stringRedisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    private String getType(String key) {
        var dataType = stringRedisTemplate.type(key);
        String type = dataType == null ? null : dataType.code();
        return type == null ? "unknown" : type;
    }

    private List<String> readRawValue(String key) {
        byte[] raw = stringRedisTemplate.execute((RedisCallback<byte[]>) connection ->
                connection.get(key.getBytes(StandardCharsets.UTF_8)));
        if (raw == null) {
            return List.of();
        }
        List<String> payload = new ArrayList<>();
        payload.add(new String(raw, StandardCharsets.UTF_8));
        return payload;
    }
}
