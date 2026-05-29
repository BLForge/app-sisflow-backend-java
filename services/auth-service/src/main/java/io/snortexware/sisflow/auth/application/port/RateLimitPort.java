package io.snortexware.sisflow.auth.application.port;

import java.time.Duration;

public interface RateLimitPort {
    boolean tryConsume(String key, long limit, Duration window);
    void clear(String key);
}
