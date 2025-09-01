#!/usr/bin/env bash
set -euo pipefail

# Verify staging readiness for Auth service
# Usage: BASE_URL=http://localhost:8081 ./scripts/verify-staging.sh

BASE_URL="${BASE_URL:-http://localhost:8081}"
echo "[auth] Verificando em: $BASE_URL"

curl -fsS "$BASE_URL/actuator/health" >/dev/null && echo "[auth] ✅ actuator/health"
curl -fsS "$BASE_URL/actuator/info" >/dev/null && echo "[auth] ✅ actuator/info" || echo "[auth] ⚠️ actuator/info indisponível (ok se desabilitado)"

# JWKS público (se exposto)
if curl -fsS "$BASE_URL/.well-known/jwks.json" >/dev/null; then
  echo "[auth] ✅ JWKS disponível"
else
  echo "[auth] ⚠️ JWKS não acessível (verificar exposição/roteamento)"
fi

# Métricas/Prometheus
curl -fsS "$BASE_URL/actuator/metrics" >/dev/null && echo "[auth] ✅ actuator/metrics"
curl -fsS "$BASE_URL/actuator/prometheus" >/dev/null && echo "[auth] ✅ prometheus (se habilitado)" || echo "[auth] ⚠️ prometheus indisponível"

echo "[auth] ✅ Verificações básicas concluídas"

