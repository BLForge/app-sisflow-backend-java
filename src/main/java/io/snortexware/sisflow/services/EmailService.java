package io.snortexware.sisflow.services;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final Resend resend;
    private final String fromEmail;
    private final String baseUrl;

    public EmailService(
            @Value("${resend.api.key}") String apiKey,
            @Value("${resend.from.email}") String fromEmail,
            @Value("${app.base.url}") String baseUrl) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
        this.baseUrl = baseUrl;
    }

    public void sendConfirmationEmail(String toEmail, String token) {
        String url = baseUrl + "/email-confirmed?token=" + token;
        send(toEmail, "Confirme seu e-mail — Sisflow", buildHtml(
                "Confirme seu e-mail",
                "Clique no botão abaixo para ativar sua conta no Sisflow.",
                url, "Confirmar e-mail",
                "Este link expira em 24 horas. Se você não criou uma conta, ignore este e-mail."
        ));
    }

    public void sendPasswordResetEmail(String toEmail, String token) {
        String url = baseUrl + "/reset-password?token=" + token;
        send(toEmail, "Redefinição de senha — Sisflow", buildHtml(
                "Redefinir senha",
                "Clique no botão abaixo para definir uma nova senha no Sisflow.",
                url, "Redefinir senha",
                "Este link expira em 1 hora. Se você não solicitou a redefinição, ignore este e-mail."
        ));
    }

    private void send(String to, String subject, String html) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail).to(to).subject(subject).html(html).build();
        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Falha ao enviar e-mail", e);
        }
    }

    private String buildHtml(String title, String body, String url, String btnLabel, String footer) {
        return """
                <div style="font-family:sans-serif;max-width:480px;margin:auto">
                  <h2>%s</h2>
                  <p>%s</p>
                  <a href="%s" style="display:inline-block;padding:12px 24px;background:#6366f1;color:#fff;border-radius:6px;text-decoration:none;font-weight:600">%s</a>
                  <p style="color:#888;font-size:12px;margin-top:24px">%s</p>
                </div>
                """.formatted(title, body, url, btnLabel, footer);
    }
}
