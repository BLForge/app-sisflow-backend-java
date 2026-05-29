package io.snortexware.sisflow.notification.infrastructure.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import io.snortexware.sisflow.notification.application.port.EmailGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResendEmailGateway implements EmailGateway {

    private final Resend resend;
    private final String fromEmail;

    public ResendEmailGateway(@Value("${resend.api.key}") String apiKey,
                              @Value("${resend.from.email}") String fromEmail) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
    }

    @Override
    public void send(String recipient, String subject, String html) {
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(recipient)
                .subject(subject)
                .html(html)
                .build();
        try {
            resend.emails().send(params);
        } catch (ResendException exception) {
            log.error("Failed to send email to {}: {}", recipient, exception.getMessage());
            throw new IllegalStateException("Failed to send notification email", exception);
        }
    }
}
