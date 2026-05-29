package io.snortexware.sisflow.notification.application.port;

public interface EmailGateway {
    void send(String recipient, String subject, String html);
}
