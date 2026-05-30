<p align="center">
  <img src="assets/sisflow-logo.png" alt="SisFlow" width="320">
</p>

# SisFlow Backend

Backend do SisFlow, uma plataforma multi-tenant para service desk com autenticação JWT, RBAC, auditoria, filas de notificação e persistência relacional.

## Serviços deste repositório

- `ticket-service`: API principal de negócio.
- `auth-service`: autenticação, refresh token e fluxos de conta.
- `notification-service`: envio assíncrono de notificações.

Infra obrigatória deste backend:

- PostgreSQL
- Redis
- RabbitMQ

Nginx não faz parte deste repositório. O proxy reverso está no repositório do frontend, que publica a SPA e encaminha `/api` para estes serviços.

## Stack

- Java 17
- Spring Boot
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Redis
- RabbitMQ

## Estrutura

```text
.
├── src/                           aplicação principal
├── services/auth-service/         serviço de autenticação
├── services/notification-service/ serviço de notificações
├── docs/                          documentação técnica e de segurança
├── docker-compose.yml             stack do backend
└── assets/                        identidade visual do projeto
```

## Execução local

API principal:

```bash
./mvnw clean test
./mvnw spring-boot:run
```

Cada microserviço também pode ser executado no próprio diretório com `mvn spring-boot:run`.

## Docker

1. Crie o arquivo de ambiente:

```bash
cp .env.example .env
```

2. Suba a stack do backend:

```bash
docker compose up --build
```

Portas padrão:

- `8080`: `ticket-service`
- `8081`: `auth-service`
- `8082`: `notification-service`
- `5432`: PostgreSQL
- `6379`: Redis
- `5672`: RabbitMQ
- `15672`: painel do RabbitMQ

Observações:

- O `ticket-service` escuta internamente em `9090`.
- O `auth-service` escuta internamente em `9091`.
- O `notification-service` escuta internamente em `9092`.
- O Dockerfile principal agora expõe `9090`, alinhado com `server.port`.

## Documentação

- Visão geral técnica: [docs/README.md](docs/README.md)
- Arquitetura: [docs/microservices-ddd-architecture.md](docs/microservices-ddd-architecture.md)
- Segurança: [docs/security](docs/security)
