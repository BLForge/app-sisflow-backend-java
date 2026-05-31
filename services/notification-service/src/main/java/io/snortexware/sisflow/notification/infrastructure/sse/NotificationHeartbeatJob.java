package io.snortexware.sisflow.notification.infrastructure.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationHeartbeatJob {

    private final NotificationPublisher notificationPublisher;

    @Scheduled(fixedDelayString = "${notifications.sse.heartbeat-ms:25000}")
    public void sendHeartbeat() {
        notificationPublisher.sendHeartbeat();
    }
}
