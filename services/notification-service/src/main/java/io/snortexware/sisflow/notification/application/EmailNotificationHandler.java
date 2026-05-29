package io.snortexware.sisflow.notification.application;

import io.snortexware.sisflow.notification.application.port.EmailGateway;
import io.snortexware.sisflow.notification.domain.model.AuthEmailNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailNotificationHandler {

    private final EmailGateway emailGateway;

    @Value("${app.base.url}")
    private String baseUrl;

    public void handle(AuthEmailNotification notification) {
        if ("CONFIRM_EMAIL".equals(notification.template())) {
            String url = baseUrl + "/email-confirmed?token=" + notification.token();
            emailGateway.send(
                    notification.recipient(),
                    "Confirme seu e-mail - Sisflow",
                    html("Confirme seu e-mail",
                            "Clique no botao abaixo para ativar sua conta no Sisflow.",
                            url,
                            "Confirmar e-mail",
                            "Este link expira em 24 horas.")
            );
            return;
        }

        if ("RESET_PASSWORD".equals(notification.template())) {
            String url = baseUrl + "/reset-password?token=" + notification.token();
            emailGateway.send(
                    notification.recipient(),
                    "Redefinicao de senha - Sisflow",
                    html("Redefinir senha",
                            "Clique no botao abaixo para definir uma nova senha no Sisflow.",
                            url,
                            "Redefinir senha",
                            "Este link expira em 1 hora.")
            );
        }
    }

    private String html(String title, String body, String url, String action, String footer) {
        return """
                <div style="font-family:sans-serif;max-width:480px;margin:auto">
                  <h2>%s</h2>
                  <p>%s</p>
                  <a href="%s" style="display:inline-block;padding:12px 24px;background:#0f766e;color:#fff;border-radius:6px;text-decoration:none;font-weight:600">%s</a>
                  <p style="color:#666;font-size:12px;margin-top:24px">%s</p>
                </div>
                """.formatted(title, body, url, action, footer);
    }
}
