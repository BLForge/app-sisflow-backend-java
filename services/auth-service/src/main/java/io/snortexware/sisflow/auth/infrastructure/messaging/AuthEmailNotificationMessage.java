package io.snortexware.sisflow.auth.infrastructure.messaging;

public record AuthEmailNotificationMessage(
        String template,
        String recipient,
        String token
) {}
