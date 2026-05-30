# Configuration Guide

Referencia de configuracao do backend e dos servicos auxiliares.

## Arquivos

- [main-service.md](main-service.md): aplicacao principal `sisflow`
- [auth-service.md](auth-service.md): servico de autenticacao
- [notification-service.md](notification-service.md): servico de notificacoes
- [docker-compose.md](docker-compose.md): variaveis da stack Docker Compose da raiz

## Template

- Variaveis de ambiente exemplo: [.env.example](../../.env.example)

## Convencoes

- Chaves `spring.*` e `server.*` controlam infraestrutura Spring Boot.
- Chaves com `${ENV_VAR:default}` podem ser sobrescritas por variavel de ambiente.
- Secrets nao devem ser commitados. Use variaveis de ambiente ou secret manager.
