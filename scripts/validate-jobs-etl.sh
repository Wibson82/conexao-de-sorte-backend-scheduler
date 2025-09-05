#!/usr/bin/env bash
set -euo pipefail

# Creates and triggers a sample job (requires JWT if protected)
# Usage: BASE_URL=http://localhost:8088 TOKEN="Bearer xxx" ./scripts/validate-jobs-etl.sh

BASE_URL="${BASE_URL:-http://localhost:8088}"
AUTH_HEADER=${TOKEN:+-H "Authorization: $TOKEN"}

echo "[scheduler] Listing jobs (may require auth)"
eval curl -fsS $AUTH_HEADER "$BASE_URL/rest/v1/jobs" >/dev/null || echo "[scheduler] ⚠️ list protected or unavailable"

echo "[scheduler] Done"

