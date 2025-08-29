#!/bin/bash
set -euo pipefail

SECRETS_DIR="/run/secrets"
SERVICE_USER="appuser"
VAULT_NAME="kv-conexao-de-sorte"

log(){ echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] $1"; }
error(){ echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] $1" >&2; exit 1; }
create_secret_file(){ local name=$1; local value=$2; local file="$SECRETS_DIR/$name";
  if [[ -z "$value" || "$value" == "null" ]]; then log "⚠️ Skipping $name (empty)"; return; fi
  echo "$value" | sudo tee "$file" >/dev/null; sudo chown root:root "$file"; sudo chmod 0400 "$file" || true
}

main(){
  log "🔐 Setup secrets (autenticacao)"
  sudo mkdir -p "$SECRETS_DIR" && sudo chown root:root "$SECRETS_DIR" && sudo chmod 755 "$SECRETS_DIR"
  az account show >/dev/null 2>&1 || az login --identity || error "Azure login failed"

  DB_R2DBC_URL=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-r2dbc-url" --query value -o tsv 2>/dev/null || echo "")
  DB_USER=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-username" --query value -o tsv 2>/dev/null || echo "")
  DB_PASSWORD=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-password" --query value -o tsv 2>/devnull || echo "")
  if [[ -z "$DB_R2DBC_URL" || "$DB_R2DBC_URL" == "null" ]]; then
    DB_R2DBC_URL=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-url" --query value -o tsv 2>/dev/null || echo "")
  fi

  DB_NAME_SPECIFIC=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-autenticacao-database-name" --query value -o tsv 2>/dev/null || echo "")
  DB_NAME_GENERIC=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-name" --query value -o tsv 2>/dev/null || echo "")
  DB_NAME=${DB_NAME_SPECIFIC:-}; [[ -z "$DB_NAME" || "$DB_NAME" == "null" ]] && DB_NAME=${DB_NAME_GENERIC:-}
  [[ -z "$DB_NAME" || "$DB_NAME" == "null" ]] && DB_NAME=${DATABASE_NAME:-}
  [[ -z "$DB_NAME" || "$DB_NAME" == "null" ]] && DB_NAME="conexao_sorte_auth"

  if [[ -n "$DB_R2DBC_URL" && "$DB_R2DBC_URL" != "null" ]]; then
    [[ "$DB_R2DBC_URL" =~ ^r2dbc:mysql://[^/]+$ ]] && DB_R2DBC_URL="$DB_R2DBC_URL/$DB_NAME"
  fi

  create_secret_file spring.r2dbc.url "$DB_R2DBC_URL"
  create_secret_file spring.r2dbc.username "$DB_USER"
  create_secret_file spring.r2dbc.password "$DB_PASSWORD"

  if [[ -n "$DB_R2DBC_URL" && "$DB_R2DBC_URL" != "null" ]]; then
    JDBC_URL="${DB_R2DBC_URL/r2dbc:mysql:/jdbc:mysql:}"
    create_secret_file spring.flyway.url "$JDBC_URL"
  fi
  create_secret_file spring.flyway.user "$DB_USER"
  create_secret_file spring.flyway.password "$DB_PASSWORD"

  unset DB_PASSWORD
  log "✅ Secrets set in $SECRETS_DIR"
}

[[ "${BASH_SOURCE[0]}" == "${0}" ]] && { command -v az >/dev/null 2>&1 || error "Azure CLI not found"; VAULT_NAME="${AZURE_KEYVAULT_NAME:-$VAULT_NAME}"; main; }

