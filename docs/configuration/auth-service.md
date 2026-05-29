# Auth Service Configuration

Arquivo base: `services/auth-service/src/main/resources/application.properties`

| Chave | Variavel de ambiente | Default | Obrigatoria | Uso |
| --- | --- | --- | --- | --- |
| `spring.application.name` | - | `auth-service` | nao | Nome logico do servico. |
| `server.port` | - | `9091` | nao | Porta HTTP do servico de autenticacao. |
| `server.error.include-message` | - | `never` | nao | Oculta mensagem interna de erro. |
| `server.error.include-binding-errors` | - | `never` | nao | Oculta detalhes de validacao. |
| `server.error.include-stacktrace` | - | `never` | nao | Impede exposicao de stacktrace. |
| `server.error.include-exception` | - | `false` | nao | Impede exposicao de excecoes. |
| `logging.level.root` | - | `WARN` | nao | Nivel global de log. |
| `logging.level.io.snortexware` | - | `INFO` | nao | Nivel de log da aplicacao. |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | - | sim | URL JDBC do PostgreSQL. |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | - | sim | Usuario do banco. |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | - | sim | Senha do banco. |
| `spring.datasource.driver-class-name` | - | `org.postgresql.Driver` | nao | Driver JDBC. |
| `spring.jpa.hibernate.ddl-auto` | - | `none` | nao | Desabilita alteracao automatica de schema. |
| `spring.jpa.show-sql` | - | `false` | nao | Desabilita SQL em log. |
| `spring.jpa.properties.hibernate.format_sql` | - | `false` | nao | Desabilita formatacao de SQL. |
| `spring.data.redis.host` | `SPRING_DATA_REDIS_HOST` | `redis` | condicional | Host do Redis para login/rate limit. |
| `spring.data.redis.port` | `SPRING_DATA_REDIS_PORT` | `6379` | condicional | Porta do Redis. |
| `spring.data.redis.timeout` | `SPRING_DATA_REDIS_TIMEOUT` | `2s` | nao | Timeout de conexao Redis. |
| `spring.rabbitmq.host` | `SPRING_RABBITMQ_HOST` | `rabbitmq` | condicional | Host do RabbitMQ. |
| `spring.rabbitmq.port` | `SPRING_RABBITMQ_PORT` | `5672` | condicional | Porta do RabbitMQ. |
| `spring.rabbitmq.username` | `SPRING_RABBITMQ_USERNAME` | `guest` | condicional | Usuario do RabbitMQ. |
| `spring.rabbitmq.password` | `SPRING_RABBITMQ_PASSWORD` | `guest` | condicional | Senha do RabbitMQ. |
| `jwt.secret` | `JWT_SECRET` | - | sim | Secret para assinatura dos JWTs. |
| `jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `3600000` | nao | Duracao do access token em ms. |
| `jwt.refresh-expiration-days` | `JWT_REFRESH_EXPIRATION_DAYS` | `30` | nao | Duracao do refresh token em dias. |
| `app.base.domain` | `APP_BASE_DOMAIN` | `localhost` | sim | Dominio base usado na resolucao do tenant. |
| `email.confirmation.expiration-hours` | `EMAIL_CONFIRMATION_EXPIRATION_HOURS` | `24` | nao | Validade do token de confirmacao. |
| `security.login.max-attempts` | `SECURITY_LOGIN_MAX_ATTEMPTS` | `10` | nao | Tentativas por conta antes de lockout. |
| `security.login.ip-max-attempts` | `SECURITY_LOGIN_IP_MAX_ATTEMPTS` | `50` | nao | Tentativas por IP antes de lockout. |
| `security.login.rate-limit-capacity` | `SECURITY_LOGIN_RATE_LIMIT_CAPACITY` | `30` | nao | Capacidade do bucket de login. |
| `security.login.rate-limit-window-minutes` | `SECURITY_LOGIN_RATE_LIMIT_WINDOW_MINUTES` | `1` | nao | Janela do bucket de login. |
| `security.login.lockout-duration-minutes` | `SECURITY_LOGIN_LOCKOUT_DURATION_MINUTES` | `15` | nao | Duracao do bloqueio temporario. |
| `notifications.exchange` | `NOTIFICATIONS_EXCHANGE` | `sisflow.notifications` | nao | Exchange usada para publicar eventos de e-mail. |
| `notifications.email.routing-key` | `NOTIFICATIONS_EMAIL_ROUTING_KEY` | `email.auth` | nao | Routing key das notificacoes de autenticacao. |
