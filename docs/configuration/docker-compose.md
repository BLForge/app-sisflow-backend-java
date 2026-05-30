# Docker Compose Environment

## Observacao importante

A stack raiz **nao** consome `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` ou `SPRING_DATASOURCE_PASSWORD` diretamente no backend.

Ela monta a conexao do backend a partir destas variaveis:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`

O backend recebe internamente:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB}
SPRING_DATASOURCE_USERNAME=${POSTGRES_USER}
SPRING_DATASOURCE_PASSWORD=${POSTGRES_PASSWORD}
```

Entao, se o objetivo for usar a stack completa local com PostgreSQL, Redis e RabbitMQ em containers, o `.env.docker` precisa declarar `POSTGRES_*`.

## Variaveis da stack raiz

| Variavel | Default sugerido | Obrigatoria | Uso |
| --- | --- | --- | --- |
| `POSTGRES_DB` | `sisflow` | sim | Nome do banco criado no container PostgreSQL. |
| `POSTGRES_USER` | `postgres` | sim | Usuario do banco local da stack. |
| `POSTGRES_PASSWORD` | `postgres` | sim | Senha do banco local da stack. |
| `POSTGRES_PORT` | `5432` | nao | Porta publicada do PostgreSQL. |
| `REDIS_PORT` | `6379` | nao | Porta publicada do Redis. |
| `RABBITMQ_PORT` | `5672` | nao | Porta AMQP publicada do RabbitMQ. |
| `RABBITMQ_MANAGEMENT_PORT` | `15672` | nao | Porta do painel de administracao do RabbitMQ. |
| `JWT_SECRET` | `change-me` | sim | Secret compartilhado entre `ticket-service` e `auth-service`. |
| `JWT_EXPIRATION_MS` | `3600000` | nao | Duracao do access token. |
| `JWT_REFRESH_EXPIRATION_DAYS` | `30` | nao | Duracao do refresh token. |
| `APP_BASE_URL` | `http://localhost:5173` | sim | URL publica usada em links, assets e e-mails. |
| `APP_BASE_DOMAIN` | `localhost` | sim | Dominio base para resolucao de tenant. |
| `APP_TRUST_HOST_HEADER` | `false` | nao | Ativa validacao por host confiavel em cenarios com proxy. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | nao | Origins permitidas pelo backend principal. |
| `CORS_ALLOWED_ORIGIN_PATTERNS` | `http://*.localhost:8080` | nao | Patterns adicionais de CORS. |
| `SWAGGER_ENABLED` | `true` | nao | Liga Swagger para ambiente local. |
| `VITE_API_BASE_URL` | `/api` | sim | Base usada pelo frontend para chamar o proxy/API. |
| `RESEND_API_KEY` | vazio | condicional | Necessaria apenas para envio real de e-mails. |
| `RESEND_FROM_EMAIL` | `noreply@example.com` | condicional | Remetente dos e-mails. |
| `SPRING_CACHE_REDIS_TIME_TO_LIVE` | `10m` | nao | TTL do cache do backend principal. |
| `SPRING_DATA_REDIS_TIMEOUT` | `2s` | nao | Timeout do Redis. |
| `SPRING_RABBITMQ_USERNAME` | `guest` | nao | Usuario AMQP. |
| `SPRING_RABBITMQ_PASSWORD` | `guest` | nao | Senha AMQP. |
| `SECURITY_LOGIN_MAX_ATTEMPTS` | `10` | nao | Tentativas por conta antes do bloqueio. |
| `SECURITY_LOGIN_IP_MAX_ATTEMPTS` | `50` | nao | Tentativas por IP antes do bloqueio. |
| `SECURITY_LOGIN_RATE_LIMIT_CAPACITY` | `30` | nao | Capacidade do bucket de login. |
| `SECURITY_LOGIN_RATE_LIMIT_WINDOW_MINUTES` | `1` | nao | Janela do bucket de login. |
| `SECURITY_LOGIN_LOCKOUT_DURATION_MINUTES` | `15` | nao | Duracao do lockout de login. |
| `NOTIFICATIONS_EXCHANGE` | `sisflow.notifications` | nao | Exchange usada entre auth e notification service. |
| `NOTIFICATIONS_EMAIL_QUEUE` | `notification.email.auth` | nao | Fila consumida pelo notification service. |
| `NOTIFICATIONS_EMAIL_ROUTING_KEY` | `email.auth` | nao | Routing key das notificacoes. |
| `TICKET_SERVICE_PORT` | `8080` | nao | Porta publicada da API principal. |
| `AUTH_SERVICE_PORT` | `8081` | nao | Porta publicada do auth-service. |
| `NOTIFICATION_SERVICE_PORT` | `8082` | nao | Porta publicada do notification-service. |
| `FRONTEND_PORT` | `5173` | nao | Porta publicada do frontend/proxy. |

## Exemplo funcional

```dotenv
POSTGRES_DB=sisflow
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
JWT_SECRET=change-me
APP_BASE_URL=http://localhost:5173
APP_BASE_DOMAIN=localhost
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://127.0.0.1:5173
SWAGGER_ENABLED=true
VITE_API_BASE_URL=/api
RESEND_API_KEY=
RESEND_FROM_EMAIL=noreply@example.com
```
