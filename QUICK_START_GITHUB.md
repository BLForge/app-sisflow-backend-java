# Quick Start - Integração GitHub

## Passo a Passo Rápido

### 1. Criar Status Personalizado (se necessário)

```bash
POST /ticket-status-config
{
  "name": "Em Homologação",
  "description": "Ticket em processo de homologação",
  "color": "#FFA500"
}
```

Guarde o `id` retornado para usar como `pullRequestStatusId`.

### 2. Criar Projeto

```bash
POST /projects
{
  "name": "Sistema de Vendas",
  "description": "Sistema principal de vendas da empresa",
  "customerId": "uuid-do-cliente-existente",
  "githubRepository": "sistema-vendas",
  "githubOwner": "empresa-xyz",
  "pullRequestStatusId": "uuid-do-status-criado-acima"
}
```

Guarde o `id` do projeto retornado.

### 3. Criar Configuração GitHub

```bash
POST /github-configurations
{
  "projectId": "uuid-do-projeto-criado",
  "webhookSecret": "meu-secret-super-seguro-123",
  "enabled": true
}
```

### 4. Criar Sistema (Opcional)

```bash
POST /systems
{
  "name": "API Backend",
  "description": "API REST principal",
  "projectId": "uuid-do-projeto",
  "version": "1.0.0",
  "url": "https://api.exemplo.com"
}
```

### 5. Configurar Webhook no GitHub

1. Acesse: `https://github.com/empresa-xyz/sistema-vendas/settings/hooks`
2. Clique em "Add webhook"
3. Configure:
   - **Payload URL**: `https://seu-backend.com/github/webhook`
   - **Content type**: `application/json`
   - **Secret**: `meu-secret-super-seguro-123` (mesmo da configuração)
   - **Which events**: Selecione "Let me select individual events" e marque "Pull requests"
   - **Active**: ✓ Marcado

### 6. Testar

1. Crie um ticket no sistema (ex: código 1234)
2. Crie uma branch: `git checkout -b feature/nova-funcionalidade-#1234`
3. Faça commits e push
4. Abra um Pull Request com título: "Implementa nova funcionalidade #1234"
5. O ticket #1234 será automaticamente atualizado para o status configurado!

## Verificação

### Verificar se o projeto está configurado corretamente:

```bash
GET /projects/{id}
```

Deve retornar:
- `githubOwner` e `githubRepository` preenchidos
- `pullRequestStatus` com o status configurado

### Verificar configuração GitHub:

```bash
GET /github-configurations/project/{projectId}
```

Deve retornar:
- `enabled: true`
- `webhookSecret` configurado

### Verificar logs do webhook:

Após criar um PR, verifique os logs do backend para mensagens como:
```
Updated ticket #1234 status to Em Homologação for PR: https://github.com/...
```

## Troubleshooting

### Webhook não está funcionando?

1. **Verifique se o projeto existe:**
   ```bash
   GET /projects
   ```

2. **Verifique se o repositório está correto:**
   - `githubOwner` deve ser exatamente o nome da organização/usuário
   - `githubRepository` deve ser exatamente o nome do repositório

3. **Verifique se a configuração está habilitada:**
   ```bash
   GET /github-configurations/project/{projectId}
   ```
   Deve retornar `enabled: true`

4. **Verifique o formato do ticket:**
   - Use `#1234` no título ou branch do PR
   - O código deve corresponder a um ticket existente

5. **Verifique os logs do GitHub:**
   - Acesse: `https://github.com/seu-org/seu-repo/settings/hooks`
   - Clique no webhook
   - Veja "Recent Deliveries" para ver as requisições e respostas

### Ticket não está sendo atualizado?

1. **Verifique se o ticket existe:**
   ```bash
   GET /tickets
   ```
   Procure pelo código usado no PR

2. **Verifique se o status está configurado:**
   ```bash
   GET /projects/{id}
   ```
   Deve ter `pullRequestStatusId` preenchido

3. **Teste manualmente o webhook:**
   ```bash
   chmod +x test-webhook.sh
   ./test-webhook.sh http://localhost:8080
   ```

## Exemplo Completo com cURL

```bash
# 1. Criar status
STATUS_ID=$(curl -X POST http://localhost:8080/ticket-status-config \
  -H "Content-Type: application/json" \
  -d '{"name":"Em Homologação","description":"Aguardando homologação","color":"#FFA500"}' \
  | jq -r '.id')

# 2. Criar projeto (assumindo que já tem um customer)
PROJECT_ID=$(curl -X POST http://localhost:8080/projects \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"Meu Sistema\",\"customerId\":\"$CUSTOMER_ID\",\"githubOwner\":\"minha-org\",\"githubRepository\":\"meu-repo\",\"pullRequestStatusId\":\"$STATUS_ID\"}" \
  | jq -r '.id')

# 3. Criar configuração GitHub
curl -X POST http://localhost:8080/github-configurations \
  -H "Content-Type: application/json" \
  -d "{\"projectId\":\"$PROJECT_ID\",\"webhookSecret\":\"meu-secret\",\"enabled\":true}"

echo "Configuração concluída!"
echo "Project ID: $PROJECT_ID"
echo "Status ID: $STATUS_ID"
```

## Próximos Passos

- Configure o webhook no GitHub
- Crie tickets e PRs para testar
- Monitore os logs para verificar o funcionamento
- Ajuste os status conforme necessário
