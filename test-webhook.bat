@echo off
REM Script Batch para testar o webhook do GitHub
REM Uso: test-webhook.bat [URL_BASE]

setlocal

if "%1"=="" (
    set BASE_URL=http://localhost:8080
) else (
    set BASE_URL=%1
)

echo Testando webhook do GitHub...
echo URL: %BASE_URL%/github/webhook
echo.

curl -X POST "%BASE_URL%/github/webhook" ^
  -H "Content-Type: application/json" ^
  -H "X-GitHub-Event: pull_request" ^
  -d @WEBHOOK_TEST_PAYLOAD.json ^
  -v

echo.
echo Teste concluido!

endlocal
