# Main Service Configuration

Arquivo base: `src/main/resources/application.properties`

| Chave | Variavel de ambiente | Default | Obrigatoria | Uso |
| --- | --- | --- | --- | --- |
| `spring.application.name` | - | `sisflow` | nao | Identificacao da aplicacao principal. |
| `server.port` | - | `9090` | nao | Porta HTTP do backend principal. |
| `server.error.include-message` | - | `never` | nao | Oculta mensagens internas em respostas de erro. |
| `server.error.include-binding-errors` | - | `never` | nao | Oculta detalhes de validacao no payload de erro. |
| `server.error.include-stacktrace` | - | `never` | nao | Impede exposicao de stacktrace. |
| `server.error.include-exception` | - | `false` | nao | Impede exposicao do nome da excecao. |
| `logging.level.org.springframework.web` | - | `WARN` | nao | Reduz ruido de logs web. |
| `logging.level.org.springframework.web.servlet.DispatcherServlet` | - | `WARN` | nao | Reduz logs do dispatcher. |
| `logging.level.org.flywaydb` | - | `WARN` | nao | Reduz logs do Flyway. |
| `logging.level.root` | - | `WARN` | nao | Nivel global de log. |
| `logging.level.io.snortexware` | - | `INFO` | nao | Nivel de log da aplicacao. |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | - | sim | URL JDBC do PostgreSQL. |
| `spring.datasource.username` | `SPRING_DATASOURCE_USERNAME` | - | sim | Usuario do banco. |
| `spring.datasource.password` | `SPRING_DATASOURCE_PASSWORD` | - | sim | Senha do banco. |
| `spring.datasource.driver-class-name` | - | `org.postgresql.Driver` | nao | Driver JDBC usado pela aplicacao. |
| `spring.datasource.hikari.data-source-properties.prepareThreshold` | - | `0` | nao | Ajuste de compatibilidade/performace do driver PostgreSQL. |
| `spring.datasource.hikari.connection-init-sql` | - | `SET search_path TO public` | nao | Define schema padrao nas conexoes. |
| `app.base.url` | `APP_BASE_URL` | `http://localhost:9090` | sim | URL publica do backend para links e arquivos. |
| `app.base.domain` | `APP_BASE_DOMAIN` | `localhost` | sim | Dominio base para resolucao de tenant por subdominio. |
| `app.trust-host-header` | `APP_TRUST_HOST_HEADER` | `false` | nao | Habilita validacao por `Host`/proxy confiavel. |
| `server.forward-headers-strategy` | - | `none` | nao | Mantem controle manual de headers de proxy. |
| `spring.servlet.multipart.enabled` | - | `true` | nao | Habilita upload multipart. |
| `spring.servlet.multipart.max-file-size` | - | `5MB` | nao | Limite por arquivo. |
| `spring.servlet.multipart.max-request-size` | - | `6MB` | nao | Limite por requisicao multipart. |
| `resend.api.key` | `RESEND_API_KEY` | vazio | condicional | Chave da Resend para envio de e-mail. |
| `resend.from.email` | `RESEND_FROM_EMAIL` | `noreply@example.com` | condicional | Remetente usado em e-mails transacionais. |
| `email.confirmation.expiration-hours` | `EMAIL_CONFIRMATION_EXPIRATION_HOURS` | `24` | nao | Validade do token de confirmacao. |
| `jwt.secret` | `JWT_SECRET` | - | sim | Secret para assinatura dos JWTs. |
| `jwt.expiration-ms` | `JWT_EXPIRATION_MS` | `3600000` | nao | Duracao do access token em ms. |
| `jwt.refresh-expiration-days` | `JWT_REFRESH_EXPIRATION_DAYS` | `30` | nao | Duracao do refresh token em dias. |
| `spring.jpa.hibernate.ddl-auto` | - | `none` | nao | Evita alteracao automatica de schema. |
| `spring.jpa.show-sql` | - | `false` | nao | Desabilita SQL em log. |
| `spring.jpa.properties.hibernate.format_sql` | - | `false` | nao | Desabilita formatacao de SQL no log. |
| `spring.flyway.enabled` | - | `true` | nao | Habilita migrations Flyway. |
| `spring.flyway.locations` | - | `classpath:db/migration,classpath:db.migration` | nao | Locais onde o Flyway procura migrations. |
| `spring.flyway.baseline-on-migrate` | - | `true` | nao | Permite baseline em bases existentes. |
| `spring.flyway.baseline-version` | - | `0` | nao | Versao inicial do baseline. |
| `spring.flyway.out-of-order` | - | `true` | nao | Permite executar migrations fora de ordem. |
| `spring.flyway.default-schema` | - | `public` | nao | Schema padrao do Flyway. |
| `spring.flyway.schemas` | - | `public` | nao | Schemas gerenciados pelo Flyway. |
| `spring.cache.type` | - | `redis` | nao | Provider de cache. |
| `spring.cache.redis.time-to-live` | `SPRING_CACHE_REDIS_TIME_TO_LIVE` | `10m` | nao | TTL padrao do cache Redis. |
| `spring.cache.redis.cache-null-values` | - | `false` | nao | Evita cache de nulos. |
| `spring.data.redis.host` | `SPRING_DATA_REDIS_HOST` | `localhost` | condicional | Host do Redis. |
| `spring.data.redis.port` | `SPRING_DATA_REDIS_PORT` | `6379` | condicional | Porta do Redis. |
| `spring.data.redis.timeout` | `SPRING_DATA_REDIS_TIMEOUT` | `2s` | nao | Timeout de conexao Redis. |
| `springdoc.api-docs.enabled` | `SWAGGER_ENABLED` | `false` | nao | Habilita endpoint OpenAPI. |
| `springdoc.swagger-ui.enabled` | `SWAGGER_ENABLED` | `false` | nao | Habilita Swagger UI. |
| `cors.allowed.origins` | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | nao | Origins exatas permitidas. |
| `cors.allowed.origin-patterns` | `CORS_ALLOWED_ORIGIN_PATTERNS` | `http://*.localhost:8080` | nao | Patterns adicionais de CORS. |
| `security.api-rate-limit.enabled` | - | `true` | nao | Liga rate limiting por API. |
| `security.api-rate-limit.user.read.client` | - | `600` | nao | Leitura por cliente autenticado. |
| `security.api-rate-limit.user.write.client` | - | `120` | nao | Escrita por cliente autenticado. |
| `security.api-rate-limit.user.read.developer` | - | `900` | nao | Leitura por developer. |
| `security.api-rate-limit.user.write.developer` | - | `180` | nao | Escrita por developer. |
| `security.api-rate-limit.user.read.moderator` | - | `1200` | nao | Leitura por moderator. |
| `security.api-rate-limit.user.write.moderator` | - | `240` | nao | Escrita por moderator. |
| `security.api-rate-limit.user.read.admin` | - | `1800` | nao | Leitura por admin. |
| `security.api-rate-limit.user.write.admin` | - | `360` | nao | Escrita por admin. |
| `security.api-rate-limit.tenant.read` | - | `3000` | nao | Leitura agregada por tenant. |
| `security.api-rate-limit.tenant.write` | - | `900` | nao | Escrita agregada por tenant. |
| `security.api-rate-limit.api-key.read` | - | `1200` | nao | Leitura por chave tecnica. |
| `security.api-rate-limit.api-key.write` | - | `300` | nao | Escrita por chave tecnica. |
| `security.login.max-attempts` | `SECURITY_LOGIN_MAX_ATTEMPTS` | `10` | nao | Tentativas por conta antes de bloqueio. |
| `security.login.ip-max-attempts` | `SECURITY_LOGIN_IP_MAX_ATTEMPTS` | `50` | nao | Tentativas por IP antes de bloqueio. |
| `security.login.rate-limit-capacity` | `SECURITY_LOGIN_RATE_LIMIT_CAPACITY` | `30` | nao | Capacidade do bucket de login. |
| `security.login.rate-limit-window-minutes` | `SECURITY_LOGIN_RATE_LIMIT_WINDOW_MINUTES` | `1` | nao | Janela do bucket de login. |
| `security.login.lockout-duration-minutes` | `SECURITY_LOGIN_LOCKOUT_DURATION_MINUTES` | `15` | nao | Duracao do lockout de login. |
| `file.upload.base-dir` | `FILE_UPLOAD_BASE_DIR` | `./uploads` | nao | Diretório fisico dos uploads expostos em `/files/**`. |
| `BOOTSTRAP_ADMIN_EMAIL` | `BOOTSTRAP_ADMIN_EMAIL` | vazio | opcional | Se preenchido, ativa semeadura do primeiro admin. |
| `BOOTSTRAP_ADMIN_PASSWORD` | `BOOTSTRAP_ADMIN_PASSWORD` | vazio | opcional | Senha inicial do admin bootstrap. |
| `BOOTSTRAP_ADMIN_NAME` | `BOOTSTRAP_ADMIN_NAME` | `Admin` | opcional | Nome do admin bootstrap. |
