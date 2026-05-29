package io.snortexware.sisflow.notification.domain.model;

public record AuthEmailNotification(
        String template,
        String recipient,
        String token
) {}
