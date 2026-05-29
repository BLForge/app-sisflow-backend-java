package io.snortexware.sisflow.auth.application.port;

public interface NotificationPort {
    void publishConfirmationEmail(String recipient, String token);
    void publishPasswordResetEmail(String recipient, String token);
}
