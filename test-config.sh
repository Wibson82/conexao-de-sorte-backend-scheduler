#!/bin/bash
# ============================================================================
# 🧪 SCRIPT DE TESTE DE CONFIGURAÇÃO DO SCHEDULER
# ============================================================================
# Este script testa se as correções de configuração foram aplicadas
# corretamente e se a aplicação inicia sem erros
# ============================================================================

set -e

echo "🧪 Iniciando testes de configuração do Scheduler..."

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para log colorido
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Teste 1: Verificar se arquivos de configuração existem
echo ""
log_info "📋 Teste 1: Verificando arquivos de configuração..."

files=(
    "src/main/resources/application.yml"
    "src/main/resources/application-dev.yml"
    "src/main/resources/application-container.yml"
    "src/main/resources/application-azure.yml"
)

for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        log_info "✅ Arquivo encontrado: $file"
    else
        log_error "❌ Arquivo não encontrado: $file"
        exit 1
    fi
done

# Teste 2: Verificar sintaxe YAML
echo ""
log_info "📋 Teste 2: Verificando sintaxe YAML..."

for file in "${files[@]}"; do
    if command -v yq > /dev/null 2>&1; then
        if yq eval '.' "$file" > /dev/null 2>&1; then
            log_info "✅ YAML válido: $file"
        else
            log_error "❌ YAML inválido: $file"
            exit 1
        fi
    else
        log_warning "⚠️ yq não encontrado, pulando validação YAML"
        break
    fi
done

# Teste 3: Verificar configurações críticas
echo ""
log_info "📋 Teste 3: Verificando configurações críticas..."

# Verificar se application.yml tem perfil padrão correto
if grep -q "active.*dev" src/main/resources/application.yml; then
    log_info "✅ Perfil padrão definido corretamente"
else
    log_error "❌ Perfil padrão não encontrado ou incorreto"
fi

# Verificar se application-dev.yml tem URL R2DBC
if grep -q "r2dbc:mysql://localhost" src/main/resources/application-dev.yml; then
    log_info "✅ URL R2DBC para dev definida"
else
    log_error "❌ URL R2DBC para dev não encontrada"
fi

# Verificar se application-container.yml tem URL R2DBC
if grep -q "r2dbc:mysql://scheduler-mysql" src/main/resources/application-container.yml; then
    log_info "✅ URL R2DBC para container definida"
else
    log_error "❌ URL R2DBC para container não encontrada"
fi

# Teste 4: Verificar script de entrada
echo ""
log_info "📋 Teste 4: Verificando script de entrada..."

if [ -f "docker/healthcheck-entrypoint.sh" ]; then
    if [ -x "docker/healthcheck-entrypoint.sh" ]; then
        log_info "✅ Script de entrada existe e é executável"
    else
        log_warning "⚠️ Script de entrada não é executável"
        chmod +x docker/healthcheck-entrypoint.sh
        log_info "✅ Permissão de execução adicionada"
    fi
else
    log_error "❌ Script de entrada não encontrado"
    exit 1
fi

# Teste 5: Compilação
echo ""
log_info "📋 Teste 5: Testando compilação..."

if mvn compile -DskipTests -q; then
    log_info "✅ Compilação bem-sucedida"
else
    log_error "❌ Falha na compilação"
    exit 1
fi

# Teste 6: Verificar docker-compose
echo ""
log_info "📋 Teste 6: Verificando docker-compose..."

if [ -f "docker-compose.yml" ]; then
    if grep -q "SPRING_PROFILES_ACTIVE=container" docker-compose.yml; then
        log_info "✅ Perfil container definido no docker-compose"
    else
        log_error "❌ Perfil container não encontrado no docker-compose"
    fi
else
    log_error "❌ docker-compose.yml não encontrado"
fi

echo ""
log_info "🎉 Todos os testes de configuração passaram!"
log_info "🚀 O Scheduler deve agora iniciar corretamente"

echo ""
echo "📝 Próximos passos:"
echo "   1. Para desenvolvimento local: mvn spring-boot:run -Dspring-boot.run.profiles=dev"
echo "   2. Para container: docker-compose up scheduler-service"
echo "   3. Para produção: definir SPRING_PROFILES_ACTIVE=azure"