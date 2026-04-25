#!/bin/bash

# Script para testar o webhook do GitHub
# Uso: ./test-webhook.sh [URL_BASE]

BASE_URL="${1:-http://localhost:8080}"

echo "Testando webhook do GitHub..."
echo "URL: ${BASE_URL}/github/webhook"
echo ""

curl -X POST "${BASE_URL}/github/webhook" \
  -H "Content-Type: application/json" \
  -H "X-GitHub-Event: pull_request" \
  -d @WEBHOOK_TEST_PAYLOAD.json \
  -v

echo ""
echo "Teste concluído!"
