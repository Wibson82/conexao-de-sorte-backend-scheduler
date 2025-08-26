#!/bin/bash
# =============================================================================
# 🔐 SETUP SECRETS - CONFIGURAÇÃO SEGURA DE SEGREDOS NO SERVIDOR
# =============================================================================
# Script para configurar secrets do Azure Key Vault no servidor de produção
# usando o padrão /run/secrets com configtree do Spring Boot
# =============================================================================

set -euo pipefail

# ===== CONFIGURAÇÕES =====
SECRETS_DIR="/run/secrets"
SERVICE_USER="appuser"
VAULT_NAME="kv-conexao-de-sorte"  # Placeholder - será configurado via env
LOG_FILE="/var/log/setup-secrets-autenticacao.log"

# ===== FUNÇÕES =====
log() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [AUTENTICACAO] $1" | tee -a "$LOG_FILE"
}

error() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] [AUTENTICACAO] $1" | tee -a "$LOG_FILE" >&2
    exit 1
}

create_secret_file() {
    local name=$1
    local value=$2
    local file="$SECRETS_DIR/$name"
    
    if [[ -z "$value" || "$value" == "null" ]]; then
        log "⚠️ Skipping $name (empty or null value)"
        return
    fi
    
    log "📝 Creating secret file: $name"
    
    # Create secret file with secure permissions
    echo "$value" | sudo tee "$file" > /dev/null
    
    # Set ownership and permissions
    sudo chown root:root "$file"
    sudo chmod 0400 "$file"
    
    # Allow app user to read via ACL (if available) or group
    if command -v setfacl >/dev/null 2>&1; then
        sudo setfacl -m u:$SERVICE_USER:r "$file" || {
            log "⚠️ ACL not available, using group permissions"
            sudo chgrp $SERVICE_USER "$file"
            sudo chmod 0440 "$file"
        }
    else
        sudo chgrp $SERVICE_USER "$file"
        sudo chmod 0440 "$file"
    fi
    
    log "✅ Secret $name created successfully"
}

# ===== MAIN EXECUTION =====
main() {
    log "🔐 Starting secure secrets setup for autenticacao microservice..."
    
    # Create service user if not exists
    if ! id "$SERVICE_USER" >/dev/null 2>&1; then
        log "👤 Creating service user: $SERVICE_USER"
        sudo useradd -r -s /bin/false -M "$SERVICE_USER"
    fi
    
    # Create secrets directory
    log "📁 Setting up secrets directory: $SECRETS_DIR"
    sudo mkdir -p "$SECRETS_DIR"
    sudo chown root:root "$SECRETS_DIR"
    sudo chmod 755 "$SECRETS_DIR"
    
    # Authenticate with Azure using Managed Identity or Service Principal
    log "🔐 Authenticating with Azure..."
    if ! az account show >/dev/null 2>&1; then
        # Try managed identity first
        az login --identity || {
            error "❌ Azure authentication failed. Ensure managed identity or service principal is configured."
        }
    fi
    
    log "🔍 Fetching secrets from Azure Key Vault: $VAULT_NAME"
    
    # Fetch and create secret files
    # Database secrets
    log "🗄️ Processing database secrets..."
    DB_URL=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-url" --query value -o tsv 2>/dev/null || echo "")
    DB_USER=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-username" --query value -o tsv 2>/dev/null || echo "")
    DB_PASSWORD=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-password" --query value -o tsv 2>/dev/null || echo "")
    
    create_secret_file "DB_URL" "$DB_URL"
    create_secret_file "DB_USER" "$DB_USER"
    create_secret_file "DB_PASSWORD" "$DB_PASSWORD"
    
    # JWT secrets (base64 decode for keys) - MICROSERVIÇO DE AUTENTICAÇÃO PRECISA DE TODAS AS CHAVES
    log "🔑 Processing JWT secrets (full key suite for authentication service)..."
    JWT_SIGNING_KEY_B64=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-jwt-signing-key" --query value -o tsv 2>/dev/null || echo "")
    JWT_VERIFICATION_KEY_B64=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-jwt-verification-key" --query value -o tsv 2>/dev/null || echo "")
    JWT_KEY_ID=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-jwt-key-id" --query value -o tsv 2>/dev/null || echo "")
    JWT_SECRET=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-jwt-secret" --query value -o tsv 2>/dev/null || echo "")
    
    # Decode base64 keys if they exist
    if [[ -n "$JWT_SIGNING_KEY_B64" && "$JWT_SIGNING_KEY_B64" != "null" ]]; then
        JWT_SIGNING_KEY=$(echo "$JWT_SIGNING_KEY_B64" | base64 -d)
        create_secret_file "JWT_SIGNING_KEY" "$JWT_SIGNING_KEY"
    fi
    
    if [[ -n "$JWT_VERIFICATION_KEY_B64" && "$JWT_VERIFICATION_KEY_B64" != "null" ]]; then
        JWT_VERIFICATION_KEY=$(echo "$JWT_VERIFICATION_KEY_B64" | base64 -d)
        create_secret_file "JWT_VERIFICATION_KEY" "$JWT_VERIFICATION_KEY"
    fi
    
    create_secret_file "JWT_KEY_ID" "$JWT_KEY_ID"
    create_secret_file "JWT_SECRET" "$JWT_SECRET"
    
    # OAuth2 secrets (específico para microserviço de autenticação)
    log "🔐 Processing OAuth2 secrets..."
    OAUTH2_CLIENT_ID=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-security-oauth2-client-id" --query value -o tsv 2>/dev/null || echo "")
    OAUTH2_CLIENT_SECRET=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-security-oauth2-client-secret" --query value -o tsv 2>/dev/null || echo "")
    
    create_secret_file "OAUTH2_CLIENT_ID" "$OAUTH2_CLIENT_ID"
    create_secret_file "OAUTH2_CLIENT_SECRET" "$OAUTH2_CLIENT_SECRET"
    
    # Encryption secrets
    log "🔐 Processing encryption secrets..."
    ENCRYPTION_MASTER_KEY=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-encryption-master-key" --query value -o tsv 2>/dev/null || echo "")
    create_secret_file "ENCRYPTION_MASTER_KEY" "$ENCRYPTION_MASTER_KEY"
    
    # Azure Tenant ID for OAuth2 (necessário para autenticação)
    log "🏢 Processing Azure Tenant ID..."
    AZURE_TENANT_ID_SECRET=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-azure-tenant-id" --query value -o tsv 2>/dev/null || echo "$AZURE_TENANT_ID")
    if [[ -n "$AZURE_TENANT_ID_SECRET" ]]; then
        create_secret_file "AZURE_TENANT_ID" "$AZURE_TENANT_ID_SECRET"
    fi
    
    # Clear sensitive variables from memory
    unset DB_PASSWORD OAUTH2_CLIENT_SECRET ENCRYPTION_MASTER_KEY
    unset JWT_SIGNING_KEY JWT_VERIFICATION_KEY JWT_SECRET JWT_SIGNING_KEY_B64 JWT_VERIFICATION_KEY_B64
    
    log "📋 Secrets setup verification for autenticacao microservice:"
    ls -la "$SECRETS_DIR" | grep -v -E '^\\s*total' || true
    
    log "✅ Autenticacao microservice secrets setup completed successfully!"
    log "📋 Next steps:"
    log "   1. Restart the autenticacao microservice"
    log "   2. Verify configtree is loading secrets correctly"
    log "   3. Test JWT token generation and validation"
    log "   4. Verify OAuth2 authentication flows"
    
    return 0
}

# ===== EXECUTION =====
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Check if running as root or with sudo
    if [[ $EUID -ne 0 ]]; then
        error "❌ This script must be run as root or with sudo"
    fi
    
    # Validate environment
    if ! command -v az >/dev/null 2>&1; then
        error "❌ Azure CLI not found. Please install it first."
    fi
    
    # Get vault name from environment or use default
    VAULT_NAME="${AZURE_KEYVAULT_NAME:-$VAULT_NAME}"
    
    log "🚀 Starting autenticacao microservice secrets setup with vault: $VAULT_NAME"
    main "$@"
fi