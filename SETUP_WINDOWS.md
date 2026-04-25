# Setup e Testes no Windows

## Pré-requisitos

1. **Java 17+** instalado
2. **Maven** instalado (ou use o `mvnw.cmd` incluído no projeto)
3. **PostgreSQL** rodando (ou use Docker)
4. **curl** ou **PowerShell 5.1+** para testes

## 1. Configurar Banco de Dados

### Opção A: PostgreSQL Local

```powershell
# Criar banco de dados
psql -U postgres
CREATE DATABASE sisflow;
\q
```

### Opção B: Docker (Recomendado)

```powershell
docker run --name sisflow-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=sisflow -p 5432:5432 -d postgres:15
```

## 2. Configurar Variáveis de Ambiente

Crie um arquivo `.env` ou configure as variáveis no PowerShell:

```powershell
# PowerShell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/sisflow"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
$env:SUPABASE_JWKS_URL="https://wlwluotgprraezzgnnbb.supabase.co/auth/v1/.well-known/jwks.json"
$env:SUPABASE_URL="https://wlwluotgprraezzgnnbb.supabase.co"
$env:SUPABASE_ANON_KEY="sua-chave-aqui"
$env:SUPABASE_STORAGE_URL="https://wlwluotgprraezzgnnbb.supabase.co/storage/v1"
$env:APP_BASE_URL="http://localhost:8080"
$env:SWAGGER_ENABLED="true"
$env:CORS_ALLOWED_ORIGINS="http://localhost:5173,http://localhost:3000"
```

Ou crie um arquivo `set-env.ps1`:

```powershell
# set-env.ps1
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/sisflow"
$env:SPRING_DATASOURCE_USERNAME="postgres"
$env:SPRING_DATASOURCE_PASSWORD="postgres"
# ... outras variáveis
```

E execute: `.\set-env.ps1`

## 3. Compilar e Executar

### Usando Maven Wrapper (Recomendado)

```powershell
# Compilar
.\mvnw.cmd clean install -DskipTests

# Executar
.\mvnw.cmd spring-boot:run
```

### Usando Maven Instalado

```powershell
# Compilar
mvn clean install -DskipTests

# Executar
mvn spring-boot:run
```

O servidor estará rodando em: `http://localhost:8080`

## 4. Testar a API

### Opção A: PowerShell (Recomendado)

```powershell
# Testar webhook
.\test-webhook.ps1

# Ou especificar URL
.\test-webhook.ps1 http://localhost:8080
```

### Opção B: curl (se instalado)

```powershell
# Testar webhook
.\test-webhook.bat

# Ou especificar URL
.\test-webhook.bat http://localhost:8080
```

### Opção C: PowerShell Manual

```powershell
# Criar um cliente
$customer = Invoke-RestMethod -Uri "http://localhost:8080/customers" `
    -Method POST `
    -ContentType "application/json" `
    -Body '{"name":"Empresa Teste","document":"12345678000190","email":"teste@empresa.com"}'

Write-Host "Customer ID: $($customer.id)"

# Criar um status
$status = Invoke-RestMethod -Uri "http://localhost:8080/ticket-status-config" `
    -Method POST `
    -ContentType "application/json" `
    -Body '{"name":"Em Homologação","description":"Aguardando homologação","color":"#FFA500"}'

Write-Host "Status ID: $($status.id)"

# Criar um projeto
$project = Invoke-RestMethod -Uri "http://localhost:8080/projects" `
    -Method POST `
    -ContentType "application/json" `
    -Body "{`"name`":`"Sistema Teste`",`"customerId`":`"$($customer.id)`",`"githubOwner`":`"minha-org`",`"githubRepository`":`"meu-repo`",`"pullRequestStatusId`":`"$($status.id)`"}"

Write-Host "Project ID: $($project.id)"

# Criar configuração GitHub
$config = Invoke-RestMethod -Uri "http://localhost:8080/github-configurations" `
    -Method POST `
    -ContentType "application/json" `
    -Body "{`"projectId`":`"$($project.id)`",`"webhookSecret`":`"meu-secret`",`"enabled`":true}"

Write-Host "GitHub Config ID: $($config.id)"

# Listar projetos
$projects = Invoke-RestMethod -Uri "http://localhost:8080/projects" -Method GET
$projects | Format-Table
```

## 5. Testar Localmente com ngrok

Como o GitHub precisa acessar seu webhook, use ngrok para expor sua aplicação:

### Instalar ngrok

1. Baixe em: https://ngrok.com/download
2. Extraia o executável
3. Configure: `ngrok config add-authtoken SEU_TOKEN`

### Expor a aplicação

```powershell
# Em um terminal separado
ngrok http 8080
```

Você verá algo como:
```
Forwarding  https://abc123.ngrok.io -> http://localhost:8080
```

### Configurar webhook no GitHub

Use a URL do ngrok: `https://abc123.ngrok.io/github/webhook`

## 6. Script Completo de Setup

Crie um arquivo `setup-local.ps1`:

```powershell
# setup-local.ps1
Write-Host "=== Setup Local - Integração GitHub ===" -ForegroundColor Green

# 1. Verificar se o servidor está rodando
try {
    $health = Invoke-RestMethod -Uri "http://localhost:8080/health" -Method GET -ErrorAction Stop
    Write-Host "✓ Servidor está rodando" -ForegroundColor Green
} catch {
    Write-Host "✗ Servidor não está rodando. Execute: .\mvnw.cmd spring-boot:run" -ForegroundColor Red
    exit 1
}

# 2. Criar cliente
Write-Host "`nCriando cliente..." -ForegroundColor Cyan
$customer = Invoke-RestMethod -Uri "http://localhost:8080/customers" `
    -Method POST `
    -ContentType "application/json" `
    -Body '{"name":"Empresa Teste Local","document":"12345678000190","email":"teste@local.com"}'
Write-Host "✓ Cliente criado: $($customer.id)" -ForegroundColor Green

# 3. Criar status
Write-Host "`nCriando status..." -ForegroundColor Cyan
$status = Invoke-RestMethod -Uri "http://localhost:8080/ticket-status-config" `
    -Method POST `
    -ContentType "application/json" `
    -Body '{"name":"Em Homologação","description":"Aguardando homologação","color":"#FFA500"}'
Write-Host "✓ Status criado: $($status.id)" -ForegroundColor Green

# 4. Criar projeto
Write-Host "`nCriando projeto..." -ForegroundColor Cyan
$projectBody = @{
    name = "Sistema Teste Local"
    description = "Projeto de teste para integração GitHub"
    customerId = $customer.id
    githubOwner = "empresa-xyz"
    githubRepository = "sistema-vendas"
    pullRequestStatusId = $status.id
} | ConvertTo-Json

$project = Invoke-RestMethod -Uri "http://localhost:8080/projects" `
    -Method POST `
    -ContentType "application/json" `
    -Body $projectBody
Write-Host "✓ Projeto criado: $($project.id)" -ForegroundColor Green

# 5. Criar configuração GitHub
Write-Host "`nCriando configuração GitHub..." -ForegroundColor Cyan
$configBody = @{
    projectId = $project.id
    webhookSecret = "meu-secret-local-123"
    enabled = $true
} | ConvertTo-Json

$config = Invoke-RestMethod -Uri "http://localhost:8080/github-configurations" `
    -Method POST `
    -ContentType "application/json" `
    -Body $configBody
Write-Host "✓ Configuração criada: $($config.id)" -ForegroundColor Green

# 6. Criar ticket de teste
Write-Host "`nCriando ticket de teste..." -ForegroundColor Cyan
$ticketBody = @{
    title = "Ticket de teste para PR"
    description = "Este ticket será atualizado pelo webhook"
    customerId = $customer.id
    priority = "medium"
    type = "bug"
} | ConvertTo-Json

$ticket = Invoke-RestMethod -Uri "http://localhost:8080/tickets" `
    -Method POST `
    -ContentType "application/json" `
    -Body $ticketBody
Write-Host "✓ Ticket criado: #$($ticket.code)" -ForegroundColor Green

# 7. Resumo
Write-Host "`n=== Setup Concluído ===" -ForegroundColor Green
Write-Host "Customer ID: $($customer.id)"
Write-Host "Project ID: $($project.id)"
Write-Host "Status ID: $($status.id)"
Write-Host "Ticket Code: #$($ticket.code)"
Write-Host "`nPara testar o webhook, edite WEBHOOK_TEST_PAYLOAD.json"
Write-Host "e altere o número do ticket para: #$($ticket.code)"
Write-Host "`nDepois execute: .\test-webhook.ps1"
```

Execute: `.\setup-local.ps1`

## 7. Verificar Logs

```powershell
# Ver logs em tempo real (se estiver rodando em outro terminal)
# Os logs aparecerão no terminal onde você executou mvnw spring-boot:run
```

## Troubleshooting Windows

### Erro: "Execution of scripts is disabled"

```powershell
# Execute como Administrador
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Erro: "curl não encontrado"

Use PowerShell em vez de curl, ou instale curl:
```powershell
# Instalar curl via Chocolatey
choco install curl

# Ou use o script PowerShell
.\test-webhook.ps1
```

### Erro: "mvnw.cmd não encontrado"

Certifique-se de estar na pasta raiz do projeto:
```powershell
cd sisflow-backend
.\mvnw.cmd spring-boot:run
```

### Porta 8080 já em uso

```powershell
# Encontrar processo usando a porta
netstat -ano | findstr :8080

# Matar processo (substitua PID pelo número encontrado)
taskkill /PID <PID> /F

# Ou use outra porta
$env:SERVER_PORT="8081"
.\mvnw.cmd spring-boot:run
```

## Próximos Passos

1. Execute o setup: `.\setup-local.ps1`
2. Teste o webhook: `.\test-webhook.ps1`
3. Configure ngrok para testes com GitHub real
4. Monitore os logs para ver as atualizações dos tickets
