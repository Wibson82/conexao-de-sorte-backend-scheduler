#!/bin/bash
set -euo pipefail

# OIDC-only policy: This script is intentionally a no-op.
# Secrets must be provided by the CI/CD pipeline via OIDC and resolved
# at runtime by Spring Cloud Azure Key Vault. Any attempt to fetch
# secrets with Azure CLI/Managed Identity at runtime is forbidden.

echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] OIDC-only mode: scripts/setup-secrets.sh is disabled."
echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] Ensure AZURE_KEYVAULT_ENABLED=true and AZURE_TENANT_ID, AZURE_CLIENT_ID, AZURE_KEYVAULT_ENDPOINT are set."
exit 0
