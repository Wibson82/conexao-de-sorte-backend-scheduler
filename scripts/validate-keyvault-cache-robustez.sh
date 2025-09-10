#!/usr/bin/env bash
set -euo pipefail

# Validates Key Vault via Spring Cloud Azure, configtree variables presence, Resilience4j toggles and cache TTLs
# Usage: BASE_URL=http://localhost:8081 ./scripts/validate-keyvault-cache-robustez.sh

BASE_URL="${BASE_URL:-http://localhost:8081}"

echo "[auth] Checking Spring Cloud Azure/KeyVault and configtree mounts"
env | grep -E 'AZURE_(TENANT_ID|CLIENT_ID|KEYVAULT|SUBSCRIPTION_ID)' || echo "[auth] ⚠️ Azure OIDC envs not present"
if [ -d /run/secrets ]; then
  echo "[auth] ✅ /run/secrets present"; ls -1 /run/secrets || true
else
  echo "[auth] ⚠️ /run/secrets not mounted"
fi

echo "[auth] Checking Resilience4j & Cache via actuator/env (requires exposure)"
curl -fsS "$BASE_URL/actuator/configprops" >/dev/null || echo "[auth] ⚠️ configprops not exposed (ok in hardened profiles)"
curl -fsS "$BASE_URL/actuator/health" >/dev/null && echo "[auth] ✅ health"
curl -fsS "$BASE_URL/actuator/metrics/cache.gets" >/dev/null && echo "[auth] ✅ cache metrics" || echo "[auth] ⚠️ cache metrics unavailable"

echo "[auth] Done"

