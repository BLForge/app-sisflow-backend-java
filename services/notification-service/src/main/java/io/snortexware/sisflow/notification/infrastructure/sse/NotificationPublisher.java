package io.snortexware.sisflow.notification.infrastructure.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final NotificationRegistry notificationRegistry;

    public void sendConnected(SseEmitter emitter, UUID userId) throws IOException {
        emitter.send(SseEmitter.event()
                .name("connected")
                .data(new ConnectionEvent("connected", userId, OffsetDateTime.now())));
    }

    public void sendNotification(UUID userId, Object payload) {
        for (SseEmitter emitter : notificationRegistry.getEmitters(userId)) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(payload));
            } catch (Exception exception) {
                emitter.completeWithError(exception);
                notificationRegistry.remove(userId, emitter);
            }
        }
    }

    public void sendHeartbeat() {
        HeartbeatEvent heartbeat = new HeartbeatEvent("heartbeat", OffsetDateTime.now());

        notificationRegistry.getEmittersMap().forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(heartbeat));
                } catch (Exception exception) {
                    emitter.completeWithError(exception);
                    notificationRegistry.remove(userId, emitter);
                }
            }
        });
    }

    public record ConnectionEvent(String status, UUID userId, OffsetDateTime occurredAt) {
    }

    public record HeartbeatEvent(String type, OffsetDateTime occurredAt) {
    }
}
