#!/usr/bin/env bash
# Seed local Vault (started via docker-compose) with the payment gateway secret.
# Usage:
#   docker compose up -d vault
#   ./scripts/vault-bootstrap.sh
set -euo pipefail

VAULT_ADDR="${VAULT_ADDR:-http://localhost:8200}"
VAULT_TOKEN="${VAULT_TOKEN:-dev-root-token}"
API_KEY="${PAYMENT_GATEWAY_API_KEY:-local-dev-key-$(date +%s)}"

echo "→ writing secret/payment-gateway to ${VAULT_ADDR}"
curl -fsS \
  -H "X-Vault-Token: ${VAULT_TOKEN}" \
  -H "Content-Type: application/json" \
  -X POST \
  -d "{\"data\":{\"api-key\":\"${API_KEY}\"}}" \
  "${VAULT_ADDR}/v1/secret/data/payment-gateway" \
  >/dev/null

echo "✓ secret/payment-gateway api-key=${API_KEY}"
