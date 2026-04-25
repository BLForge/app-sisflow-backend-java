# Script para testar endpoints
# Uso: .\test-endpoints.ps1

$BASE_URL = "http://localhost:8080"

Write-Host "=== Testando Endpoints ===" -ForegroundColor Green
Write-Host ""

# 1. Health check
Write-Host "1. Health Check..." -ForegroundColor Cyan
try {
    $health = Invoke-RestMethod -Uri "$BASE_URL/health" -Method GET
    Write-Host "✓ Health OK" -ForegroundColor Green
} catch {
    Write-Host "✗ Health falhou: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 2. Login (ajuste email/senha conforme necessário)
Write-Host "2. Fazendo login..." -ForegroundColor Cyan
$loginBody = @{
    email = "admin@example.com"
    password = "password123"
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BASE_URL/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.accessToken
    Write-Host "✓ Login OK - Token obtido" -ForegroundColor Green
} catch {
    Write-Host "✗ Login falhou: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Ajuste o email/senha no script" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Headers com token
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# 3. Listar customers
Write-Host "3. Listando customers..." -ForegroundColor Cyan
try {
    $customers = Invoke-RestMethod -Uri "$BASE_URL/customers" -Method GET -Headers $headers
    Write-Host "✓ Customers OK - Total: $($customers.Count)" -ForegroundColor Green
    if ($customers.Count -gt 0) {
        $customerId = $customers[0].id
        Write-Host "  Usando customer: $($customers[0].name) ($customerId)" -ForegroundColor Gray
    }
} catch {
    Write-Host "✗ Customers falhou: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 4. Criar sistema
Write-Host "4. Criando sistema..." -ForegroundColor Cyan
if ($customerId) {
    $systemBody = @{
        name = "Sistema Teste $(Get-Date -Format 'HHmmss')"
        description = "Sistema de teste criado via script"
        customerId = $customerId
        version = "1.0.0"
        url = "https://teste.com"
    } | ConvertTo-Json

    try {
        $system = Invoke-RestMethod -Uri "$BASE_URL/systems" -Method POST -Body $systemBody -Headers $headers
        Write-Host "✓ Sistema criado: $($system.name) ($($system.id))" -ForegroundColor Green
        $systemId = $system.id
    } catch {
        Write-Host "✗ Criar sistema falhou: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
        
        # Tentar ler o corpo da resposta
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "Response: $responseBody" -ForegroundColor Yellow
        } catch {}
    }
} else {
    Write-Host "⊘ Pulando - nenhum customer disponível" -ForegroundColor Yellow
}

Write-Host ""

# 5. Listar sistemas
Write-Host "5. Listando sistemas..." -ForegroundColor Cyan
try {
    $systems = Invoke-RestMethod -Uri "$BASE_URL/systems" -Method GET -Headers $headers
    Write-Host "✓ Sistemas OK - Total: $($systems.Count)" -ForegroundColor Green
} catch {
    Write-Host "✗ Listar sistemas falhou: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""

# 6. Criar projeto
Write-Host "6. Criando projeto..." -ForegroundColor Cyan
if ($systemId) {
    $projectBody = @{
        name = "Projeto Teste $(Get-Date -Format 'HHmmss')"
        description = "Projeto de teste"
        systemId = $systemId
        githubOwner = "test-org"
        githubRepository = "test-repo"
    } | ConvertTo-Json

    try {
        $project = Invoke-RestMethod -Uri "$BASE_URL/projects" -Method POST -Body $projectBody -Headers $headers
        Write-Host "✓ Projeto criado: $($project.name) ($($project.id))" -ForegroundColor Green
    } catch {
        Write-Host "✗ Criar projeto falhou: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    }
} else {
    Write-Host "⊘ Pulando - nenhum sistema disponível" -ForegroundColor Yellow
}

Write-Host ""

# 7. Criar artigo base de conhecimento
Write-Host "7. Criando artigo base de conhecimento..." -ForegroundColor Cyan
$articleBody = @{
    title = "Artigo Teste $(Get-Date -Format 'HHmmss')"
    content = "Conteúdo de teste do artigo"
    isPublished = $true
} | ConvertTo-Json

try {
    $article = Invoke-RestMethod -Uri "$BASE_URL/knowledge-base" -Method POST -Body $articleBody -Headers $headers
    Write-Host "✓ Artigo criado: $($article.title) ($($article.id))" -ForegroundColor Green
} catch {
    Write-Host "✗ Criar artigo falhou: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Testes Concluídos ===" -ForegroundColor Green
