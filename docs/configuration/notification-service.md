# Notification Service Configuration

Arquivo base: `services/notification-service/src/main/resources/application.properties`

| Chave | Variavel de ambiente | Default | Obrigatoria | Uso |
| --- | --- | --- | --- | --- |
| `spring.application.name` | - | `notification-service` | nao | Nome logico do servico. |
| `server.port` | - | `9092` | nao | Porta HTTP do servico de notificacoes. |
| `logging.level.root` | - | `WARN` | nao | Nivel global de log. |
| `logging.level.io.snortexware` | - | `INFO` | nao | Nivel de log da aplicacao. |
| `spring.rabbitmq.host` | `SPRING_RABBITMQ_HOST` | `rabbitmq` | condicional | Host do RabbitMQ. |
| `spring.rabbitmq.port` | `SPRING_RABBITMQ_PORT` | `5672` | condicional | Porta do RabbitMQ. |
| `spring.rabbitmq.username` | `SPRING_RABBITMQ_USERNAME` | `guest` | condicional | Usuario do RabbitMQ. |
| `spring.rabbitmq.password` | `SPRING_RABBITMQ_PASSWORD` | `guest` | condicional | Senha do RabbitMQ. |
| `notifications.exchange` | `NOTIFICATIONS_EXCHANGE` | `sisflow.notifications` | nao | Exchange de notificacoes. |
| `notifications.email.queue` | `NOTIFICATIONS_EMAIL_QUEUE` | `notification.email.auth` | nao | Fila consumida pelo servico. |
| `notifications.email.routing-key` | `NOTIFICATIONS_EMAIL_ROUTING_KEY` | `email.auth` | nao | Routing key vinculada a fila. |
| `resend.api.key` | `RESEND_API_KEY` | vazio | sim em producao | Chave da Resend para envio de e-mails. |
| `resend.from.email` | `RESEND_FROM_EMAIL` | `noreply@example.com` | sim em producao | Remetente dos e-mails enviados. |
| `app.base.url` | `APP_BASE_URL` | `http://localhost:9090` | sim | URL publica usada nos links de e-mail. |
