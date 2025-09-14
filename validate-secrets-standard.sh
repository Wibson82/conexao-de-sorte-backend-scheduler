#!/bin/bash
# ============================================================================
# 🔍 SCRIPT DE VALIDAÇÃO DA PADRONIZAÇÃO DE SEGREDOS
# ============================================================================
# Este script valida se o projeto está seguindo a padronização de segredos
# conforme definido em SEGREDOS_PADRONIZADOS.md
# ============================================================================

set -e

echo "🔍 Validando padronização de segredos do Scheduler..."

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função para log colorido
log_info() {
    echo -e "${GREEN}[✅ PASS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[⚠️  WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[❌ FAIL]${NC} $1"
}

log_checking() {
    echo -e "${BLUE}[🔍 CHECK]${NC} $1"
}

# Contadores
PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

# Função para incrementar contadores
pass() {
    log_info "$1"
    ((PASS_COUNT++))
}

fail() {
    log_error "$1"
    ((FAIL_COUNT++))
}

warn() {
    log_warning "$1"
    ((WARN_COUNT++))
}

echo ""
echo "📋 Verificando conformidade com padrão híbrido (Docker Secrets + Azure Key Vault)..."

# Teste 1: Verificar padrão híbrido no application.yml
echo ""
log_checking "Teste 1: Padrão híbrido no application.yml"

if grep -q '\${REDIS_HOST:\${conexao-de-sorte-redis-host:' src/main/resources/application.yml; then
    pass "Redis Host usa padrão híbrido"
else
    fail "Redis Host não usa padrão híbrido"
fi

if grep -q '\${DATABASE_R2DBC_URL:\${conexao-de-sorte-database-r2dbc-url:' src/main/resources/application.yml; then
    pass "Database R2DBC URL usa padrão híbrido"
else
    fail "Database R2DBC URL não usa padrão híbrido"
fi

if grep -q '\${JWT_ISSUER:\${conexao-de-sorte-jwt-issuer:' src/main/resources/application.yml; then
    pass "JWT Issuer usa padrão híbrido"
else
    fail "JWT Issuer não usa padrão híbrido"
fi

if grep -q '\${SSL_ENABLED:\${conexao-de-sorte-ssl-enabled:' src/main/resources/application.yml; then
    pass "SSL Enabled usa padrão híbrido"
else
    fail "SSL Enabled não usa padrão híbrido"
fi

# Teste 2: Verificar padrão híbrido no application-dev.yml
echo ""
log_checking "Teste 2: Padrão híbrido no application-dev.yml"

if grep -q '\${REDIS_HOST:\${conexao-de-sorte-redis-host:' src/main/resources/application-dev.yml; then
    pass "Dev: Redis Host usa padrão híbrido"
else
    fail "Dev: Redis Host não usa padrão híbrido"
fi

if grep -q '\${DATABASE_USERNAME:\${conexao-de-sorte-database-username:' src/main/resources/application-dev.yml; then
    pass "Dev: Database Username usa padrão híbrido"
else
    fail "Dev: Database Username não usa padrão híbrido"
fi

# Teste 3: Verificar padrão híbrido no application-container.yml
echo ""
log_checking "Teste 3: Padrão híbrido no application-container.yml"

if grep -q '\${REDIS_HOST:\${conexao-de-sorte-redis-host:' src/main/resources/application-container.yml; then
    pass "Container: Redis Host usa padrão híbrido"
else
    fail "Container: Redis Host não usa padrão híbrido"
fi

if grep -q '\${DATABASE_PASSWORD:\${conexao-de-sorte-database-password:' src/main/resources/application-container.yml; then
    pass "Container: Database Password usa padrão híbrido"
else
    fail "Container: Database Password não usa padrão híbrido"
fi

# Teste 4: Verificar padrão híbrido no application-azure.yml
echo ""
log_checking "Teste 4: Padrão híbrido no application-azure.yml"

if grep -q '\${REDIS_HOST:\${conexao-de-sorte-redis-host' src/main/resources/application-azure.yml; then
    pass "Azure: Redis Host usa padrão híbrido"
else
    fail "Azure: Redis Host não usa padrão híbrido"
fi

if grep -q '\${DATABASE_R2DBC_URL:\${conexao-de-sorte-database-r2dbc-url' src/main/resources/application-azure.yml; then
    pass "Azure: Database R2DBC URL usa padrão híbrido"
else
    fail "Azure: Database R2DBC URL não usa padrão híbrido"
fi

# Teste 5: Verificar padrão híbrido no docker-compose.yml
echo ""
log_checking "Teste 5: Padrão híbrido no docker-compose.yml"

if grep -q 'REDIS_HOST=\${REDIS_HOST:-' docker-compose.yml; then
    pass "Docker Compose: Redis Host usa padrão híbrido"
else
    fail "Docker Compose: Redis Host não usa padrão híbrido"
fi

if grep -q 'DATABASE_R2DBC_URL=\${DATABASE_R2DBC_URL:-' docker-compose.yml; then
    pass "Docker Compose: Database R2DBC URL usa padrão híbrido"
else
    fail "Docker Compose: Database R2DBC URL não usa padrão híbrido"
fi

if grep -q 'JWT_ISSUER=\${JWT_ISSUER:-' docker-compose.yml; then
    pass "Docker Compose: JWT Issuer usa padrão híbrido"
else
    fail "Docker Compose: JWT Issuer não usa padrão híbrido"
fi

# Teste 6: Verificar nomenclatura de Docker Secrets (SNAKE_CASE)
echo ""
log_checking "Teste 6: Nomenclatura Docker Secrets (SNAKE_CASE maiúsculo)"

secrets_snake_case=(
    "REDIS_HOST"
    "REDIS_PORT"
    "REDIS_PASSWORD"
    "REDIS_DATABASE"
    "DATABASE_R2DBC_URL"
    "DATABASE_JDBC_URL"
    "DATABASE_USERNAME"
    "DATABASE_PASSWORD"
    "JWT_ISSUER"
    "JWT_JWKS_URI"
    "SSL_ENABLED"
    "CORS_ALLOWED_ORIGINS"
    "CORS_ALLOW_CREDENTIALS"
)

for secret in "${secrets_snake_case[@]}"; do
    if grep -q "\${$secret:" src/main/resources/application*.yml docker-compose.yml; then
        pass "Docker Secret '$secret' está em SNAKE_CASE"
    else
        warn "Docker Secret '$secret' não encontrado ou não está em SNAKE_CASE"
    fi
done

# Teste 7: Verificar nomenclatura Azure Key Vault (kebab-case)
echo ""
log_checking "Teste 7: Nomenclatura Azure Key Vault (kebab-case com prefixo)"

keyvault_secrets=(
    "conexao-de-sorte-redis-host"
    "conexao-de-sorte-redis-port"
    "conexao-de-sorte-redis-password"
    "conexao-de-sorte-database-r2dbc-url"
    "conexao-de-sorte-database-username"
    "conexao-de-sorte-database-password"
    "conexao-de-sorte-jwt-issuer"
    "conexao-de-sorte-jwt-jwks-uri"
    "conexao-de-sorte-ssl-enabled"
)

for secret in "${keyvault_secrets[@]}"; do
    if grep -q "\${$secret" src/main/resources/application*.yml docker-compose.yml; then
        pass "Azure Key Vault '$secret' está em kebab-case com prefixo"
    else
        fail "Azure Key Vault '$secret' não encontrado"
    fi
done

# Teste 8: Verificar se há segredos não padronizados (legados)
echo ""
log_checking "Teste 8: Verificando segredos legados não padronizados"

legacy_patterns=(
    "conexao-de-sorte-db-host"
    "conexao-de-sorte-db-port"
    "conexao-de-sorte-database-url"
)

for pattern in "${legacy_patterns[@]}"; do
    if grep -q "$pattern" src/main/resources/application*.yml docker-compose.yml; then
        warn "Encontrado segredo legado não padronizado: $pattern"
    else
        pass "Segredo legado '$pattern' não encontrado (bom!)"
    fi
done

# Relatório final
echo ""
echo "═══════════════════════════════════════════════════════════════════════"
echo "📊 RELATÓRIO DE PADRONIZAÇÃO DE SEGREDOS"
echo "═══════════════════════════════════════════════════════════════════════"
echo ""
echo -e "${GREEN}✅ PASSOU: $PASS_COUNT testes${NC}"
echo -e "${YELLOW}⚠️  AVISOS: $WARN_COUNT testes${NC}"
echo -e "${RED}❌ FALHOU: $FAIL_COUNT testes${NC}"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    echo -e "${GREEN}🎉 PARABÉNS! O projeto está seguindo a padronização de segredos!${NC}"
    echo ""
    echo "✅ Padrão híbrido implementado corretamente"
    echo "✅ Docker Secrets em SNAKE_CASE"
    echo "✅ Azure Key Vault em kebab-case com prefixo"
    echo "✅ Fallback automático funcionando"
    exit 0
else
    echo -e "${RED}❌ O projeto NÃO está seguindo completamente a padronização!${NC}"
    echo ""
    echo "📝 Ações necessárias:"
    echo "   1. Corrigir as falhas identificadas acima"
    echo "   2. Implementar padrão híbrido onde necessário"
    echo "   3. Executar este script novamente para validar"
    exit 1
fi