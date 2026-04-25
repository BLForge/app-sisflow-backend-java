# Script PowerShell para testar o webhook do GitHub
# Uso: .\test-webhook.ps1 [URL_BASE]

param(
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "Testando webhook do GitHub..." -ForegroundColor Green
Write-Host "URL: $BaseUrl/github/webhook" -ForegroundColor Cyan
Write-Host ""

$payload = Get-Content -Path "WEBHOOK_TEST_PAYLOAD.json" -Raw

try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/github/webhook" `
        -Method POST `
        -Headers @{
            "Content-Type" = "application/json"
            "X-GitHub-Event" = "pull_request"
        } `
        -Body $payload `
        -UseBasicParsing

    Write-Host "Status Code: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response:" -ForegroundColor Yellow
    Write-Host $response.Content
} catch {
    Write-Host "Erro ao fazer requisição:" -ForegroundColor Red
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body:" -ForegroundColor Yellow
        Write-Host $responseBody
    }
}

Write-Host ""
Write-Host "Teste concluído!" -ForegroundColor Green
