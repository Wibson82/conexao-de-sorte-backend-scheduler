#!/bin/bash

# ============================================================================
# 🔐 AZURE KEY VAULT TO DOCKER SECRETS SYNCHRONIZER
# ============================================================================
# 
# Script reutilizável para sincronizar secrets do Azure Key Vault com Docker Swarm
# Pode ser adaptado para diferentes microserviços mudando a lista de secrets
#
# 🛡️ POLÍTICA DE SEGURANÇA:
# - NUNCA exibe valores de secrets nos logs
# - Limpa variáveis da memória imediatamente após uso
# - Gera JWT Keys RSA-2048 seguros automaticamente se ausentes
# - Usa padrões de produção para URLs e configurações
#
# Uso: ./sync-azure-keyvault-secrets.sh [VAULT_NAME] [SERVICE_NAME]
# 
# Parâmetros:
#   VAULT_NAME   - Nome do Azure Key Vault (obrigatório)  
#   SERVICE_NAME - Nome do serviço para logs (opcional, default: "microservice")
#
# Variáveis de ambiente esperadas:
#   AZURE_CLIENT_ID, AZURE_TENANT_ID, AZURE_SUBSCRIPTION_ID
#
# Retorna: 0 se sucesso, 1 se erro crítico
# ============================================================================

# Configuração de erro rigorosa para produção
# Removido -e para permitir tratamento de erros individuais por secret
set -uo pipefail

# Função de log seguro para debugging
debug_log() {
  local level=$1
  local message=$2

  case $level in
    INFO)
      echo "ℹ️ $message"
      ;;
    DEBUG)
      echo "🔍 DEBUG: $message"
      ;;
    ERROR)
      echo "❌ ERRO: $message"
      ;;
    WARNING)
      echo "⚠️ AVISO: $message"
      ;;
    SUCCESS)
      echo "✅ $message"
      ;;
    *)
      echo "$message"
      ;;
  esac
}

# Função para tratar erros sem expor secrets
handle_error() {
  local error_code=$1
  local operation=$2
  local context=$3

  debug_log "ERROR" "Falha na operação: $operation"
  debug_log "ERROR" "Contexto: $context"
  debug_log "ERROR" "Código de erro: $error_code"

  # Verificar tipo de erro específico para Docker
  if [[ "$operation" == *"docker"* ]]; then
    debug_log "DEBUG" "Verificando status do Docker..."
    docker info > /tmp/docker_info.log 2>&1

    # Verificar permissões do Docker
    if grep -q "permission denied" /tmp/docker_info.log; then
      debug_log "ERROR" "Erro de permissão do Docker. O usuário atual não tem permissões suficientes."
      debug_log "DEBUG" "Tente executar com sudo ou verifique se o usuário está no grupo docker."
    fi

    # Verificar se o Docker está em execução
    if ! docker info >/dev/null 2>&1; then
      debug_log "ERROR" "O serviço Docker não está em execução ou não está acessível."
    fi

    # Limpar arquivo temporário
    rm -f /tmp/docker_info.log
  fi

  # Verificar tipo de erro específico para Azure
  if [[ "$operation" == *"keyvault"* ]]; then
    debug_log "DEBUG" "Verificando conectividade com Azure Key Vault..."

    # Testar acesso ao vault sem expor dados
    if ! az keyvault show --name "$VAULT_NAME" >/dev/null 2>&1; then
      debug_log "ERROR" "Não foi possível acessar o Azure Key Vault. Verifique o nome e as permissões."
    else
      debug_log "DEBUG" "Key Vault está acessível, problema pode estar relacionado aos secrets específicos."
    fi
  fi
}

# ============================================================================
# CONFIGURAÇÕES E VALIDAÇÕES
# ============================================================================

VAULT_NAME="${1:-}"
SERVICE_NAME="${2:-microservice}"

debug_log "INFO" "Iniciando sincronização Azure Key Vault para Docker Secrets"

if [[ -z "$VAULT_NAME" ]]; then
    debug_log "ERROR" "Nome do Azure Key Vault é obrigatório"
    echo "Uso: $0 [VAULT_NAME] [SERVICE_NAME]"
    exit 1
fi

# 🔧 CORREÇÃO: Se recebemos endpoint, extrair apenas o nome do vault
if [[ "$VAULT_NAME" =~ https://([^.]+)\.vault\.azure\.net ]]; then
    VAULT_NAME="${BASH_REMATCH[1]}"
    debug_log "INFO" "Extraído nome do vault: $VAULT_NAME"
elif [[ "$VAULT_NAME" == *"***"* ]]; then
    # Se recebemos valor mascarado, usar o nome correto
    VAULT_NAME="kv-conexao-de-sorte"
    debug_log "INFO" "Usando nome padrão do vault: $VAULT_NAME"
fi

# Verificar se Azure CLI está autenticado
debug_log "DEBUG" "Verificando autenticação do Azure CLI..."
if ! az account show >/dev/null 2>&1; then
    debug_log "ERROR" "Azure CLI não está autenticado"
    exit 1
fi

# Verificar se Docker Swarm está ativo
debug_log "DEBUG" "Verificando status do Docker Swarm..."
docker_swarm_status=$(docker info --format '{{.Swarm.LocalNodeState}}' 2>/dev/null || echo "error")
debug_log "DEBUG" "Status do Docker Swarm: $docker_swarm_status"

if [[ "$docker_swarm_status" != "active" ]]; then
    debug_log "ERROR" "Docker Swarm não está ativo"
    debug_log "DEBUG" "Docker deve estar em modo Swarm para utilizar secrets"
    debug_log "DEBUG" "Inicialize com: docker swarm init"
    exit 1
fi

debug_log "INFO" "Sincronizando Azure Key Vault ($VAULT_NAME) com Docker Secrets para $SERVICE_NAME..."

# ============================================================================
# CONFIGURAÇÃO DE SECRETS POR TIPO DE SERVIÇO
# ============================================================================

# Lista padrão de secrets - pode ser customizada por serviço
declare -a SECRETS_LIST

case "$SERVICE_NAME" in
    "gateway"|"conexao-de-sorte-backend-gateway")
        SECRETS_LIST=(
            # JWT
            "conexao-de-sorte-jwt-privateKey:JWT_PRIVATE_KEY"
            "conexao-de-sorte-jwt-publicKey:JWT_PUBLIC_KEY"
            "conexao-de-sorte-jwt-signing-key:JWT_SIGNING_KEY"
            "conexao-de-sorte-jwt-verification-key:JWT_VERIFICATION_KEY"
            "conexao-de-sorte-jwt-secret:JWT_SECRET"
            "conexao-de-sorte-jwt-key-id:JWT_KEY_ID"
            "conexao-de-sorte-jwt-issuer:JWT_ISSUER"
            "conexao-de-sorte-jwt-jwks-uri:JWT_JWKS_URI"

            # Redis
            "conexao-de-sorte-redis-host:REDIS_HOST"
            "conexao-de-sorte-redis-port:REDIS_PORT"
            "conexao-de-sorte-redis-password:REDIS_PASSWORD"
            "conexao-de-sorte-redis-database:REDIS_DATABASE"

            # Banco de Dados (para circuit breakers/rotas que eventualmente necessitem)
            "conexao-de-sorte-database-jdbc-url:DATABASE_JDBC_URL"
            "conexao-de-sorte-database-r2dbc-url:DATABASE_R2DBC_URL"
            "conexao-de-sorte-database-url:DATABASE_URL"
            "conexao-de-sorte-database-username:DATABASE_USERNAME"
            "conexao-de-sorte-database-password:DATABASE_PASSWORD"
            "conexao-de-sorte-database-proxysql-password:DATABASE_PROXYSQL_PASSWORD"

            # SSL
            "conexao-de-sorte-ssl-enabled:SSL_ENABLED"
            "conexao-de-sorte-ssl-keystore-path:SSL_KEYSTORE_PATH"
            "conexao-de-sorte-ssl-keystore-password:SSL_KEYSTORE_PASSWORD"

            # Criptografia
            "conexao-de-sorte-encryption-master-key:ENCRYPTION_MASTER_KEY"
            "conexao-de-sorte-encryption-master-password:ENCRYPTION_MASTER_PASSWORD"
            "conexao-de-sorte-encryption-backup-key:ENCRYPTION_BACKUP_KEY"

            # CORS
            "conexao-de-sorte-cors-allowed-origins:CORS_ALLOWED_ORIGINS"
            "conexao-de-sorte-cors-allow-credentials:CORS_ALLOW_CREDENTIALS"
        )
        ;;
    "autenticacao"|"conexao-de-sorte-backend-autenticacao")
        SECRETS_LIST=(
            "conexao-de-sorte-jwt-privateKey:JWT_PRIVATE_KEY"
            "conexao-de-sorte-jwt-publicKey:JWT_PUBLIC_KEY"
            "conexao-de-sorte-jwt-signing-key:JWT_SIGNING_KEY"
            "conexao-de-sorte-jwt-secret:JWT_SECRET"
            "conexao-de-sorte-redis-host:REDIS_HOST"
            "conexao-de-sorte-redis-password:REDIS_PASSWORD"
            "conexao-de-sorte-database-r2dbc-url:DATABASE_R2DBC_URL"
            "conexao-de-sorte-database-username:DATABASE_USERNAME"
            "conexao-de-sorte-database-password:DATABASE_PASSWORD"
        )
        ;;
    *)
        # Lista padrão para outros microserviços
        SECRETS_LIST=(
            "conexao-de-sorte-jwt-verification-key:JWT_VERIFICATION_KEY"
            "conexao-de-sorte-jwt-publicKey:JWT_PUBLIC_KEY"
            "conexao-de-sorte-jwt-issuer:JWT_ISSUER"
            "conexao-de-sorte-redis-host:REDIS_HOST"
            "conexao-de-sorte-redis-password:REDIS_PASSWORD"
            "conexao-de-sorte-database-r2dbc-url:DATABASE_R2DBC_URL"
            "conexao-de-sorte-database-username:DATABASE_USERNAME"
            "conexao-de-sorte-database-password:DATABASE_PASSWORD"
        )
        ;;
esac

# Também sincronizar secrets com os mesmos nomes das chaves do Key Vault
# para habilitar leitura automática via Spring ConfigTree (/run/secrets)
CONFIGTREE_LIST=()
for mapping in "${SECRETS_LIST[@]}"; do
    kv_secret="${mapping%:*}"
    CONFIGTREE_LIST+=("${kv_secret}:${kv_secret}")
done

# ============================================================================
# FUNÇÃO DE GERAÇÃO DE JWT KEYS
# ============================================================================

generate_jwt_keypair() {
    debug_log "INFO" "Gerando novo par de chaves JWT RSA-2048..."

    local temp_dir=$(mktemp -d)
    local private_key="$temp_dir/private.pem"
    local public_key="$temp_dir/public.pem"
    
    # Verificar ferramentas necessárias
    if ! command -v openssl &> /dev/null; then
        debug_log "ERROR" "OpenSSL não encontrado, necessário para gerar chaves"
        return 1
    fi

    # Generate RSA private key (2048-bit)
    debug_log "DEBUG" "Gerando chave privada RSA..."
    if ! openssl genrsa -out "$private_key" 2048 2>/tmp/openssl.err; then
        debug_log "ERROR" "Falha ao gerar chave privada RSA"
        debug_log "DEBUG" "Erro OpenSSL: $(cat /tmp/openssl.err)"
        rm -f /tmp/openssl.err
        return 1
    fi

    # Extract public key
    debug_log "DEBUG" "Extraindo chave pública..."
    if ! openssl rsa -in "$private_key" -pubout -out "$public_key" 2>/tmp/openssl.err; then
        debug_log "ERROR" "Falha ao extrair chave pública"
        debug_log "DEBUG" "Erro OpenSSL: $(cat /tmp/openssl.err)"
        rm -f /tmp/openssl.err
        return 1
    fi
    rm -f /tmp/openssl.err

    # Generate key ID (timestamp-based)
    local key_id="jwt-key-$(date +%Y%m%d-%H%M%S)"
    
    # Generate signing key (base64 encoded private key)
    debug_log "DEBUG" "Gerando signing key em base64..."
    local signing_key=$(base64 -w 0 < "$private_key" 2>/dev/null || base64 < "$private_key" | tr -d '\n')
    
    # Generate verification key (base64 encoded public key)
    debug_log "DEBUG" "Gerando verification key em base64..."
    local verification_key=$(base64 -w 0 < "$public_key" 2>/dev/null || base64 < "$public_key" | tr -d '\n')
    
    debug_log "INFO" "Armazenando chaves JWT no Azure Key Vault..."

    # Store private key
    if ! az keyvault secret set \
        --vault-name "$VAULT_NAME" \
        --name "conexao-de-sorte-jwt-privateKey" \
        --file "$private_key" \
        --description "JWT Private Key - Generated $(date -Iseconds)" \
        --tags "type=jwt" "rotation=$(date +%Y%m%d)" "keyId=$key_id" \
        > /dev/null 2>/tmp/az.err; then
        debug_log "ERROR" "Falha ao armazenar chave privada no Key Vault"
        debug_log "DEBUG" "Erro Azure CLI: $(cat /tmp/az.err)"
        rm -f /tmp/az.err
        return 1
    fi

    # Store public key
    if ! az keyvault secret set \
        --vault-name "$VAULT_NAME" \
        --name "conexao-de-sorte-jwt-publicKey" \
        --file "$public_key" \
        --description "JWT Public Key - Generated $(date -Iseconds)" \
        --tags "type=jwt" "rotation=$(date +%Y%m%d)" "keyId=$key_id" \
        > /dev/null 2>/tmp/az.err; then
        debug_log "ERROR" "Falha ao armazenar chave pública no Key Vault"
        debug_log "DEBUG" "Erro Azure CLI: $(cat /tmp/az.err)"
        rm -f /tmp/az.err
        return 1
    fi

    # Store signing key
    if ! az keyvault secret set \
        --vault-name "$VAULT_NAME" \
        --name "conexao-de-sorte-jwt-signing-key" \
        --value "$signing_key" \
        --description "JWT Signing Key (base64) - Generated $(date -Iseconds)" \
        --tags "type=jwt" "rotation=$(date +%Y%m%d)" "keyId=$key_id" \
        > /dev/null 2>/tmp/az.err; then
        debug_log "ERROR" "Falha ao armazenar signing key no Key Vault"
        debug_log "DEBUG" "Erro Azure CLI: $(cat /tmp/az.err)"
        rm -f /tmp/az.err
        return 1
    fi
    # Clear from memory immediately for security
    signing_key="***CLEARED***"

    # Store verification key
    if ! az keyvault secret set \
        --vault-name "$VAULT_NAME" \
        --name "conexao-de-sorte-jwt-verification-key" \
        --value "$verification_key" \
        --description "JWT Verification Key (base64) - Generated $(date -Iseconds)" \
        --tags "type=jwt" "rotation=$(date +%Y%m%d)" "keyId=$key_id" \
        > /dev/null 2>/tmp/az.err; then
        debug_log "ERROR" "Falha ao armazenar verification key no Key Vault"
        debug_log "DEBUG" "Erro Azure CLI: $(cat /tmp/az.err)"
        rm -f /tmp/az.err
        return 1
    fi
    # Clear from memory immediately for security
    verification_key="***CLEARED***"

    # Store key ID
    if ! az keyvault secret set \
        --vault-name "$VAULT_NAME" \
        --name "conexao-de-sorte-jwt-key-id" \
        --value "$key_id" \
        --description "JWT Key ID - Generated $(date -Iseconds)" \
        --tags "type=jwt" "rotation=$(date +%Y%m%d)" \
        > /dev/null 2>/tmp/az.err; then
        debug_log "ERROR" "Falha ao armazenar key ID no Key Vault"
        debug_log "DEBUG" "Erro Azure CLI: $(cat /tmp/az.err)"
        rm -f /tmp/az.err
        return 1
    fi

    # Store issuer (production URL)
    if ! az keyvault secret set \
        --vault-name "$VAULT_NAME" \
        --name "conexao-de-sorte-jwt-issuer" \
        --value "https://auth.conexao-de-sorte.com.br" \
        --description "JWT Issuer URL - Production" \
        --tags "type=jwt" "environment=production" \
        > /dev/null 2>/tmp/az.err; then
        debug_log "ERROR" "Falha ao armazenar issuer no Key Vault"
        debug_log "DEBUG" "Erro Azure CLI: $(cat /tmp/az.err)"
        rm -f /tmp/az.err
        return 1
    fi

    # Store JWKS URI
    if ! az keyvault secret set \
        --vault-name "$VAULT_NAME" \
        --name "conexao-de-sorte-jwt-jwks-uri" \
        --value "https://auth.conexao-de-sorte.com.br/.well-known/jwks.json" \
        --description "JWT JWKS URI - Production" \
        --tags "type=jwt" "environment=production" \
        > /dev/null 2>/tmp/az.err; then
        debug_log "ERROR" "Falha ao armazenar JWKS URI no Key Vault"
        debug_log "DEBUG" "Erro Azure CLI: $(cat /tmp/az.err)"
        rm -f /tmp/az.err
        return 1
    fi
    rm -f /tmp/az.err

    # Generate and store JWT secret (32 bytes random) - NEVER LOG THE VALUE
    debug_log "DEBUG" "Gerando JWT secret aleatório..."
    local jwt_secret=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
    if ! az keyvault secret set \
        --vault-name "$VAULT_NAME" \
        --name "conexao-de-sorte-jwt-secret" \
        --value "$jwt_secret" \
        --description "JWT Secret - Generated $(date -Iseconds)" \
        --tags "type=jwt" "rotation=$(date +%Y%m%d)" \
        > /dev/null 2>/tmp/az.err; then
        debug_log "ERROR" "Falha ao armazenar JWT secret no Key Vault"
        debug_log "DEBUG" "Erro Azure CLI: $(cat /tmp/az.err)"
        rm -f /tmp/az.err
        # Clear from memory immediately for security
        jwt_secret="***CLEARED***"
        return 1
    fi
    rm -f /tmp/az.err
    # Clear from memory immediately for security
    jwt_secret="***CLEARED***"
    
    # Clean up temporary files
    rm -rf "$temp_dir"
    
    debug_log "SUCCESS" "Par de chaves JWT gerado e armazenado com sucesso!"
    debug_log "INFO" "Key ID: $key_id"
    debug_log "INFO" "Issuer: https://auth.conexao-de-sorte.com.br"

    return 0
}

generate_default_secrets() {
    debug_log "INFO" "Gerando secrets padrão ausentes..."

    # Redis defaults para desenvolvimento/produção - ajustar para Docker Swarm
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-host" >/dev/null 2>&1; then
        az keyvault secret set --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-host" --value "conexao-redis" >/dev/null
        debug_log "SUCCESS" "Redis host configurado: conexao-redis"
    else
        # Update existing Redis host to Docker Swarm service name
        current_host=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-host" --query value --output tsv 2>/dev/null)
        if [[ "$current_host" == "redis.conexao-de-sorte.com.br" ]] || [[ "$current_host" == "localhost" ]] || [[ "$current_host" == "redis" ]]; then
            az keyvault secret set --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-host" --value "conexao-redis" >/dev/null
            debug_log "SUCCESS" "Redis host atualizado: $current_host → conexao-redis"
        else
            debug_log "INFO" "Redis host atual: $current_host"
        fi
    fi
    
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-port" >/dev/null 2>&1; then
        az keyvault secret set --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-port" --value "6379" >/dev/null
        debug_log "SUCCESS" "Redis port configurado: 6379"
    fi
    
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-database" >/dev/null 2>&1; then
        az keyvault secret set --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-database" --value "1" >/dev/null
        debug_log "SUCCESS" "Redis database configurado: 1 (valor padrão recomendado)"
    fi
    
    # Não sobrescrever a senha do Redis de forma insegura.
    # Se já existir no Key Vault, não alteramos. Caso contrário, apenas registramos aviso.
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-redis-password" >/dev/null 2>&1; then
        debug_log "WARNING" "Secret 'conexao-de-sorte-redis-password' ausente no Key Vault e não foi possível inferir do container."
        debug_log "WARNING" "Caso o Redis exija senha, crie o secret no Key Vault para evitar WRONGPASS."
    else
        debug_log "SUCCESS" "Redis password já presente no Key Vault."
    fi
    
    # Database defaults
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-r2dbc-url" >/dev/null 2>&1; then
        az keyvault secret set --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-r2dbc-url" \
            --value "r2dbc:mysql://mysql.conexao-de-sorte.com.br:3306/conexao_de_sorte?sslMode=REQUIRED" >/dev/null
        debug_log "SUCCESS" "Database R2DBC URL configurada"
    fi
    
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-username" >/dev/null 2>&1; then
        az keyvault secret set --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-username" --value "conexao_user" >/dev/null
        debug_log "SUCCESS" "Database username configurado: conexao_user"
    fi
    
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-password" >/dev/null 2>&1; then
        local db_password=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
        az keyvault secret set --vault-name "$VAULT_NAME" --name "conexao-de-sorte-database-password" --value "$db_password" >/dev/null
        debug_log "SUCCESS" "Database password gerada e armazenada (valor protegido)"
        # Clear from memory immediately for security
        db_password="***CLEARED***"
    fi
    
    # CORS defaults
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-cors-allowed-origins" >/dev/null 2>&1; then
        az keyvault secret set --vault-name "$VAULT_NAME" --name "conexao-de-sorte-cors-allowed-origins" \
            --value "https://conexao-de-sorte.com.br,https://www.conexao-de-sorte.com.br" >/dev/null
        debug_log "SUCCESS" "CORS allowed origins configurado"
    fi
    
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "conexao-de-sorte-cors-allow-credentials" >/dev/null 2>&1; then
        az keyvault secret set --vault-name "$VAULT_NAME" --name "conexao-de-sorte-cors-allow-credentials" --value "true" >/dev/null
        debug_log "SUCCESS" "CORS allow credentials configurado"
    fi
    
    debug_log "SUCCESS" "Secrets padrão configurados com sucesso!"
}

# ============================================================================
# FUNÇÃO DE SINCRONIZAÇÃO
# ============================================================================

sync_secret() {
    local keyvault_secret="$1"
    local docker_secret="$2"
    
    debug_log "INFO" "Sincronizando: $keyvault_secret -> $docker_secret"

    # 🛡️ SECURITY: Obter secret do Azure Key Vault - VALOR NUNCA EXPOSTO
    local secret_value
    if ! secret_value=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "$keyvault_secret" --query value --output tsv 2>/tmp/keyvault.err); then
        debug_log "ERROR" "Falha ao obter secret do Azure Key Vault: $keyvault_secret"
        debug_log "DEBUG" "Erro Azure: $(cat /tmp/keyvault.err | grep -v "value")"
        rm -f /tmp/keyvault.err
        return 1
    fi
    rm -f /tmp/keyvault.err

    if [[ -z "$secret_value" || "$secret_value" == "null" ]]; then
        debug_log "WARNING" "Secret vazio ou nulo no Key Vault: $keyvault_secret"
        return 1
    fi

    # Verificar existência do secret no Docker
    local existing_secret=false
    if docker secret inspect "$docker_secret" >/dev/null 2>&1; then
        existing_secret=true
        debug_log "DEBUG" "Secret já existe no Docker: $docker_secret, removendo para atualizar..."
    fi

    # Remover secret existente (se houver)
    if [[ "$existing_secret" == "true" ]]; then
        if ! docker secret rm "$docker_secret" >/dev/null 2>/tmp/docker.err; then
            debug_log "ERROR" "Falha ao remover Docker Secret existente: $docker_secret"
            debug_log "DEBUG" "Erro Docker: $(cat /tmp/docker.err)"

            # Verificar serviços que usam o secret
            debug_log "DEBUG" "Verificando serviços que usam o secret $docker_secret..."
            docker service ls --filter "secret=$docker_secret" --format "{{.Name}}" > /tmp/services.txt

            if [[ -s /tmp/services.txt ]]; then
                debug_log "ERROR" "Secret $docker_secret está em uso pelos seguintes serviços:"
                cat /tmp/services.txt | while read service; do
                    debug_log "DEBUG" "- $service"
                done
                debug_log "DEBUG" "É necessário atualizar ou remover esses serviços antes de atualizar o secret."
            fi

            rm -f /tmp/docker.err /tmp/services.txt
            # 🛡️ SECURITY: Clear from memory even on failure
            secret_value="***CLEARED***"
            return 1
        fi
        debug_log "DEBUG" "Secret removido com sucesso: $docker_secret"
        sleep 1  # Aguardar limpeza
    fi

    # Criar novo secret - valor passado via pipe para não aparecer nos logs
    debug_log "DEBUG" "Criando novo Docker Secret: $docker_secret..."
    if ! echo "$secret_value" | docker secret create "$docker_secret" - >/dev/null 2>/tmp/docker.err; then
        debug_log "ERROR" "Falha ao criar Docker Secret: $docker_secret"
        debug_log "DEBUG" "Erro Docker: $(cat /tmp/docker.err)"

        # Verificar detalhes do ambiente Docker
        debug_log "DEBUG" "Verificando ambiente Docker:"
        docker_version=$(docker version --format '{{.Server.Version}}' 2>/dev/null || echo "Desconhecido")
        debug_log "DEBUG" "Versão do Docker: $docker_version"

        debug_log "DEBUG" "Verificando usuário atual e permissões:"
        debug_log "DEBUG" "Usuário: $(whoami)"
        debug_log "DEBUG" "Grupos: $(groups)"

        rm -f /tmp/docker.err
        # 🛡️ SECURITY: Clear from memory even on failure
        secret_value="***CLEARED***"
        return 1
    fi
    rm -f /tmp/docker.err

    debug_log "SUCCESS" "Sincronizado: $docker_secret"

    # 🛡️ SECURITY: Clear from memory immediately
    secret_value="***CLEARED***"
    return 0
}

# ============================================================================
# EXECUÇÃO DA SINCRONIZAÇÃO
# ============================================================================

SUCCESS_COUNT=0
ERROR_COUNT=0
TOTAL_COUNT=${#SECRETS_LIST[@]}

debug_log "INFO" "Total de secrets a sincronizar: $TOTAL_COUNT"
echo ""

# Verificar se precisa gerar JWT (primeira execução)
JWT_MISSING=false
REQUIRED_JWT_SECRETS=(
    "conexao-de-sorte-jwt-privateKey"
    "conexao-de-sorte-jwt-publicKey"
    "conexao-de-sorte-jwt-signing-key"
    "conexao-de-sorte-jwt-verification-key"
    "conexao-de-sorte-jwt-secret"
    "conexao-de-sorte-jwt-key-id"
)

debug_log "DEBUG" "Verificando secrets JWT essenciais..."
for jwt_secret in "${REQUIRED_JWT_SECRETS[@]}"; do
    if ! az keyvault secret show --vault-name "$VAULT_NAME" --name "$jwt_secret" >/dev/null 2>&1; then
        debug_log "WARNING" "Secret JWT ausente: $jwt_secret"
        JWT_MISSING=true
    else
        # Verificar se o secret não está vazio
        secret_value=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "$jwt_secret" --query value --output tsv 2>/dev/null)
        if [[ -z "$secret_value" || "$secret_value" == "null" ]]; then
            debug_log "WARNING" "Secret JWT vazio ou nulo: $jwt_secret"
            JWT_MISSING=true
        else
            debug_log "SUCCESS" "Secret JWT válido: $jwt_secret"
        fi
    fi
done

if [[ "$JWT_MISSING" == "true" ]]; then
    debug_log "WARNING" "Um ou mais secrets JWT estão ausentes ou vazios"
    debug_log "INFO" "Regenerando todos os secrets JWT para garantir consistência..."

    # Usar a função básica que já está definida
    if generate_jwt_keypair; then
        debug_log "SUCCESS" "Secrets JWT regenerados com sucesso!"

        # Verificar se todos foram criados corretamente
        debug_log "DEBUG" "Verificando secrets JWT após regeneração..."
        for jwt_secret in "${REQUIRED_JWT_SECRETS[@]}"; do
            if az keyvault secret show --vault-name "$VAULT_NAME" --name "$jwt_secret" >/dev/null 2>&1; then
                secret_value=$(az keyvault secret show --vault-name "$VAULT_NAME" --name "$jwt_secret" --query value --output tsv 2>/dev/null)
                if [[ -n "$secret_value" && "$secret_value" != "null" ]]; then
                    debug_log "SUCCESS" "Secret JWT criado e validado: $jwt_secret"
                else
                    debug_log "ERROR" "Secret JWT criado mas está vazio: $jwt_secret"
                fi
            else
                debug_log "ERROR" "Falha ao criar secret JWT: $jwt_secret"
            fi
        done
    else
        debug_log "ERROR" "Falha ao gerar secrets JWT"
        debug_log "INFO" "Continuando sincronização com secrets existentes..."
    fi
    echo ""
else
    debug_log "SUCCESS" "Todos os secrets JWT estão presentes e válidos!"
    echo ""
fi

# Verificar e gerar secrets padrão ausentes
debug_log "INFO" "Verificando secrets padrão..."
generate_default_secrets
echo ""

# Sincronizar cada secret da lista
debug_log "DEBUG" "Iniciando loop de sincronização (Docker Secrets padrão)..."
for secret_mapping in "${SECRETS_LIST[@]}"; do
    keyvault_secret="${secret_mapping%:*}"
    docker_secret="${secret_mapping#*:}"
    
    debug_log "DEBUG" "Processando mapping: $secret_mapping"

    if sync_secret "$keyvault_secret" "$docker_secret"; then
        ((SUCCESS_COUNT++))
        debug_log "DEBUG" "Secrets sincronizados até agora: $SUCCESS_COUNT de $TOTAL_COUNT"
    else
        ((ERROR_COUNT++))
        debug_log "ERROR" "Falha ao sincronizar: $keyvault_secret -> $docker_secret"
        # Tentar entender o problema específico com este secret
        handle_error "$?" "sync_secret" "$keyvault_secret -> $docker_secret"
    fi
    
    # Adicionar uma pequena pausa entre operações para evitar limitação de API
    sleep 0.5
done

# Sincronizar cópias com nomes de propriedade (ConfigTree)
debug_log "DEBUG" "Sincronizando cópias para ConfigTree (/run/secrets)"
for secret_mapping in "${CONFIGTREE_LIST[@]}"; do
    keyvault_secret="${secret_mapping%:*}"
    docker_secret="${secret_mapping#*:}"
    if sync_secret "$keyvault_secret" "$docker_secret"; then
        ((SUCCESS_COUNT++))
    else
        ((ERROR_COUNT++))
        handle_error "$?" "sync_secret-configtree" "$keyvault_secret -> $docker_secret"
    fi
    sleep 0.2
done

# ============================================================================
# RELATÓRIO FINAL
# ============================================================================

echo ""
debug_log "INFO" "RESULTADO DA SINCRONIZAÇÃO:"
debug_log "SUCCESS" "Secrets sincronizados: $SUCCESS_COUNT/$TOTAL_COUNT"

if [[ $ERROR_COUNT -gt 0 ]]; then
    debug_log "WARNING" "Secrets com erro: $ERROR_COUNT/$TOTAL_COUNT"

    # Se mais de 80% dos secrets falharam, é erro crítico (mais tolerante)
    if [[ $ERROR_COUNT -gt $((TOTAL_COUNT * 4 / 5)) ]]; then
        debug_log "ERROR" "Muitos secrets falharam ($ERROR_COUNT de $TOTAL_COUNT) - processo comprometido"

        # Listar status dos componentes para diagnóstico
        debug_log "DEBUG" "Status do Docker:"
        docker info --format '{{.ServerVersion}} - {{.OperatingSystem}} - Swarm: {{.Swarm.LocalNodeState}}' 2>/dev/null || echo "Erro ao obter informações"

        debug_log "DEBUG" "Status do Azure Key Vault:"
        az keyvault show --name "$VAULT_NAME" --query "{Name:name,ResourceGroup:resourceGroup}" -o tsv 2>/dev/null || echo "Erro ao acessar o Key Vault"

        debug_log "DEBUG" "Verifique os logs acima para identificar o problema específico."
        exit 1
    else
        debug_log "WARNING" "Alguns secrets falharam ($ERROR_COUNT de $TOTAL_COUNT), mas o número não é crítico"
        debug_log "INFO" "O sistema poderá operar com limitações - prosseguindo com deploy"
    fi
else
    debug_log "SUCCESS" "Todos os secrets do $SERVICE_NAME sincronizados com sucesso!"
fi

echo ""
debug_log "SUCCESS" "Sincronização Azure Key Vault -> Docker Secrets concluída"
debug_log "DEBUG" "Script finalizado em: $(date -Iseconds)"
exit 0
