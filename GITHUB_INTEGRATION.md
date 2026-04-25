# Integração GitHub - Projetos e Sistemas

## Visão Geral

Este sistema permite integrar o GitHub com o helpdesk para atualizar automaticamente o status dos tickets quando um Pull Request é criado ou mesclado.

## Estrutura

### Entidades

1. **Project** - Projetos do cliente
   - Vinculado a um Customer
   - Contém informações do repositório GitHub (owner/repository)
   - Define qual status aplicar quando receber um PR

2. **System** - Sistemas vinculados aos projetos
   - Cada projeto pode ter múltiplos sistemas
   - Útil para organizar diferentes módulos/aplicações

3. **GithubConfiguration** - Configuração do webhook
   - Vinculado a um Project
   - Contém webhook secret para validação
   - Pode ser habilitado/desabilitado

4. **Ticket** - Atualizado com:
   - Referência ao Project
   - Referência ao System (opcional)
   - URL do Pull Request

## Fluxo de Funcionamento

1. **Configuração Inicial**
   - Criar um Customer
   - Criar um Project vinculado ao Customer
   - Configurar o repositório GitHub (owner e repository)
   - Definir qual status aplicar quando receber um PR (pullRequestStatusId)
   - Criar uma GithubConfiguration para o projeto

2. **Webhook do GitHub**
   - Configurar webhook no GitHub apontando para: `https://seu-dominio.com/github/webhook`
   - Selecionar evento: `Pull requests`
   - O webhook envia dados quando um PR é aberto/fechado/mesclado

3. **Processamento Automático**
   - Sistema recebe o webhook
   - Identifica o projeto pelo owner/repository
   - Extrai o código do ticket do título ou branch do PR (formato: #1234)
   - Busca o ticket pelo código
   - Atualiza o status do ticket conforme configurado no projeto
   - Salva a URL do PR no ticket

## Endpoints da API

### Projects

```
POST   /projects                    - Criar projeto
GET    /projects                    - Listar todos os projetos
GET    /projects/{id}               - Buscar projeto por ID
GET    /projects/customer/{id}      - Listar projetos de um cliente
PUT    /projects/{id}               - Atualizar projeto
DELETE /projects/{id}               - Deletar projeto
```

**Exemplo de criação:**
```json
{
  "name": "Sistema de Vendas",
  "description": "Sistema principal de vendas",
  "customerId": "uuid-do-cliente",
  "githubRepository": "sistema-vendas",
  "githubOwner": "minha-empresa",
  "pullRequestStatusId": "uuid-do-status"
}
```

### Systems

```
POST   /systems                     - Criar sistema
GET    /systems                     - Listar todos os sistemas
GET    /systems/{id}                - Buscar sistema por ID
GET    /systems/project/{id}        - Listar sistemas de um projeto
PUT    /systems/{id}                - Atualizar sistema
DELETE /systems/{id}                - Deletar sistema
```

**Exemplo de criação:**
```json
{
  "name": "API Backend",
  "description": "API REST do sistema",
  "projectId": "uuid-do-projeto",
  "version": "1.0.0",
  "url": "https://api.exemplo.com"
}
```

### GitHub Configuration

```
POST   /github-configurations              - Criar configuração
GET    /github-configurations              - Listar todas as configurações
GET    /github-configurations/{id}         - Buscar por ID
GET    /github-configurations/project/{id} - Buscar por projeto
PUT    /github-configurations/{id}         - Atualizar configuração
DELETE /github-configurations/{id}         - Deletar configuração
```

**Exemplo de criação:**
```json
{
  "projectId": "uuid-do-projeto",
  "webhookSecret": "seu-secret-aqui",
  "enabled": true
}
```

### Webhook

```
POST   /github/webhook              - Recebe webhooks do GitHub
```

## Formato do Ticket no PR

O sistema busca o código do ticket no título ou branch do PR usando o padrão `#CODIGO`.

**Exemplos válidos:**
- Título: "Fix: Corrige bug no login #1234"
- Branch: "feature/adiciona-relatorio-#1234"
- Título: "Implementa nova funcionalidade (#1234)"

## Configuração no GitHub

1. Acesse as configurações do repositório
2. Vá em Settings > Webhooks > Add webhook
3. Configure:
   - **Payload URL**: `https://seu-dominio.com/github/webhook`
   - **Content type**: `application/json`
   - **Secret**: (mesmo configurado na GithubConfiguration)
   - **Events**: Selecione "Pull requests"
   - **Active**: Marcado

## Segurança

- O webhook valida se o projeto existe e está configurado
- Verifica se a integração está habilitada
- Logs detalhados para auditoria
- Suporte a webhook secret para validação (implementar validação HMAC se necessário)

## Exemplo de Uso Completo

1. Criar cliente:
```bash
POST /customers
{
  "name": "Empresa XYZ",
  "document": "12345678000190",
  ...
}
```

2. Criar projeto:
```bash
POST /projects
{
  "name": "Sistema ERP",
  "customerId": "uuid-do-cliente",
  "githubOwner": "empresa-xyz",
  "githubRepository": "erp-sistema",
  "pullRequestStatusId": "uuid-status-em-homologacao"
}
```

3. Criar configuração GitHub:
```bash
POST /github-configurations
{
  "projectId": "uuid-do-projeto",
  "webhookSecret": "meu-secret-seguro",
  "enabled": true
}
```

4. Criar ticket:
```bash
POST /tickets
{
  "title": "Corrigir bug no relatório",
  "code": 1234,
  ...
}
```

5. Criar PR no GitHub:
   - Título: "Fix: Corrige bug no relatório #1234"
   - Quando o PR for aberto, o ticket #1234 será automaticamente atualizado

## Logs

O sistema registra:
- Webhooks recebidos
- Projetos não encontrados
- Tickets atualizados
- Erros de processamento

Verifique os logs em caso de problemas.
