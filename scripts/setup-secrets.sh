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

  # Function to get secret with fallback
  get_secret() {
    local secret_name="$1"
    local fallback="$2"
    local value=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "$secret_name" --query value -o tsv 2>/dev/null || echo "")
    echo "${value:-$fallback}"
  }

  # Database secrets
  DB_R2DBC_URL=$(get_secret "conexao-de-sorte-database-r2dbc-url" "r2dbc:mysql://localhost:3306/conexao_de_sorte_auth")
  DB_USER=$(get_secret "conexao-de-sorte-database-username" "auth_user")
  DB_PASSWORD=$(get_secret "conexao-de-sorte-database-password" "auth_default_pass")

  # Redis secrets
  REDIS_HOST=$(get_secret "conexao-de-sorte-redis-host" "localhost")
  REDIS_PORT=$(get_secret "conexao-de-sorte-redis-port" "6379")
  REDIS_PASSWORD=$(get_secret "conexao-de-sorte-redis-password" "redis_password")
  REDIS_DATABASE=$(get_secret "conexao-de-sorte-redis-database" "0")

  # JWT secrets
  JWT_ISSUER=$(get_secret "conexao-de-sorte-jwt-issuer" "https://auth.conexaodesorte.com")
  JWT_JWKS_URI=$(get_secret "conexao-de-sorte-jwt-jwks-uri" "https://auth.conexaodesorte.com/.well-known/jwks.json")

  # CORS secrets
  CORS_ALLOWED_ORIGINS=$(get_secret "conexao-de-sorte-cors-allowed-origins" "*")
  CORS_ALLOW_CREDENTIALS=$(get_secret "conexao-de-sorte-cors-allow-credentials" "true")

  # SSL secrets
  SSL_ENABLED=$(get_secret "conexao-de-sorte-ssl-enabled" "false")
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

  # Create database secret files
  create_secret_file spring.r2dbc.url "$DB_R2DBC_URL"
  create_secret_file spring.r2dbc.username "$DB_USER"
  create_secret_file spring.r2dbc.password "$DB_PASSWORD"

  if [[ -n "$DB_R2DBC_URL" && "$DB_R2DBC_URL" != "null" ]]; then
    JDBC_URL="${DB_R2DBC_URL/r2dbc:mysql:/jdbc:mysql:}"
    create_secret_file spring.flyway.url "$JDBC_URL"
  fi
  create_secret_file spring.flyway.user "$DB_USER"
  create_secret_file spring.flyway.password "$DB_PASSWORD"

  # Create Redis secret files
  create_secret_file spring.data.redis.host "$REDIS_HOST"
  create_secret_file spring.data.redis.port "$REDIS_PORT"
  create_secret_file spring.data.redis.password "$REDIS_PASSWORD"
  create_secret_file spring.data.redis.database "$REDIS_DATABASE"

  # Create JWT secret files
  create_secret_file spring.security.oauth2.resourceserver.jwt.issuer-uri "$JWT_ISSUER"
  create_secret_file spring.security.oauth2.resourceserver.jwt.jwk-set-uri "$JWT_JWKS_URI"

  # Create CORS secret files
  create_secret_file cors.allowed-origins "$CORS_ALLOWED_ORIGINS"
  create_secret_file cors.allow-credentials "$CORS_ALLOW_CREDENTIALS"

  # Create SSL secret files
  create_secret_file server.ssl.enabled "$SSL_ENABLED"

  unset DB_PASSWORD REDIS_PASSWORD
  log "✅ Secrets set in $SECRETS_DIR"
}

[[ "${BASH_SOURCE[0]}" == "${0}" ]] && { command -v az >/dev/null 2>&1 || error "Azure CLI not found"; VAULT_NAME="${AZURE_KEYVAULT_NAME:-$VAULT_NAME}"; main; }

