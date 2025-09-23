#!/bin/bash
# ============================================================================
# 🐳 DOCKER ENTRYPOINT - SCHEDULER MICROSERVICE
# ============================================================================
#
# Script de inicialização personalizado para Scheduler Backend
# Contexto: Agendador de tarefas críticas (Jobs cron)
# - Validações específicas para jobs agendados
# - Health checks para schedulers
# - Retry logic validation
# - Cron job monitoring
# - Job execution tracking
# - Error handling e recovery
# - Performance monitoring para jobs
# - Resource allocation validation
#
# Uso: Configurar no Dockerfile como ENTRYPOINT
# ============================================================================

set -euo pipefail

# ============================================================================
# 📋 CONFIGURAÇÃO ESPECÍFICA DO SCHEDULER
# ============================================================================

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Função de log
log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')] [SCHEDULER]${NC} $1"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] [SCHEDULER] ERROR:${NC} $1" >&2
}

success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] [SCHEDULER] SUCCESS:${NC} $1"
}

warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] [SCHEDULER] WARNING:${NC} $1"
}

# ============================================================================
# 🔧 VALIDAÇÃO DE AMBIENTE - SCHEDULER ESPECÍFICO
# ============================================================================

log "🚀 Iniciando validação de ambiente - Scheduler Microservice..."

# Verificar se estamos rodando como usuário correto
if [[ "$(id -u)" -eq 0 ]]; then
    warning "Executando como root - isso pode ser inseguro em produção"
fi

# Variáveis obrigatórias específicas do Scheduler
required_vars=(
    "CONEXAO_DE_SORTE_DATABASE_R2DBC_URL"
    "CONEXAO_DE_SORTE_DATABASE_USERNAME"
    "CONEXAO_DE_SORTE_DATABASE_PASSWORD"
    "CONEXAO_DE_SORTE_REDIS_HOST"
    "CONEXAO_DE_SORTE_REDIS_PORT"
    "CONEXAO_DE_SORTE_REDIS_PASSWORD"
    "CONEXAO_DE_SORTE_SERVER_PORT"
    "CONEXAO_DE_SORTE_JWT_ISSUER"
)

missing_vars=()
for var in "${required_vars[@]}"; do
    if [[ -z "${!var:-}" ]]; then
        missing_vars+=("$var")
    fi
done

if [[ ${#missing_vars[@]} -gt 0 ]]; then
    error "Variáveis de ambiente obrigatórias não definidas para Scheduler:"
    for var in "${missing_vars[@]}"; do
        error "  - $var"
    fi
    exit 1
fi

# Validações específicas do Scheduler
if [[ "$CONEXAO_DE_SORTE_SERVER_PORT" != "8084" ]]; then
    warning "Porta do Scheduler diferente do padrão: $CONEXAO_DE_SORTE_SERVER_PORT (esperado: 8084)"
fi

# Validar se não está usando H2 em produção
if [[ "${SPRING_PROFILES_ACTIVE:-}" == "prod" ]]; then
    if [[ "${CONEXAO_DE_SORTE_DATABASE_R2DBC_URL:-}" =~ r2dbc:h2 ]]; then
        error "❌ H2 database detectado em ambiente de produção - violação de segurança"
        exit 1
    fi
    success "✅ Validação de segurança: H2 não detectado em produção"
fi

success "✅ Validação de ambiente concluída - Scheduler"

# ============================================================================
# ⏰ VALIDAÇÃO DE SCHEDULER SPECIFIC - JOBS
# ============================================================================

log "⏰ Executando validações específicas do Scheduler..."

# Verificar se há configuração de cron jobs
if [[ -d "/app/cron" ]] || [[ -f "/app/cron/jobs" ]]; then
    success "✅ Diretório de cron jobs encontrado"
else
    warning "⚠️ Nenhum diretório de cron jobs encontrado"
fi

# Verificar se há configuração de retry
if [[ -n "${SCHEDULER_MAX_RETRY:-}" ]]; then
    log "ℹ️ Configuração de retry detectada: $SCHEDULER_MAX_RETRY tentativas"
fi

# Verificar se há configuração de job timeout
if [[ -n "${SCHEDULER_JOB_TIMEOUT:-}" ]]; then
    log "ℹ️ Configuração de timeout detectada: $SCHEDULER_JOB_TIMEOUT"
fi

# Validar configuração de thread pool
if [[ -n "${SCHEDULER_THREAD_POOL_SIZE:-}" ]]; then
    log "ℹ️ Configuração de thread pool detectada: $SCHEDULER_THREAD_POOL_SIZE"
fi

# ============================================================================
# 🗄️ VALIDAÇÃO DE CONECTIVIDADE - DATABASE
# ============================================================================

log "🔍 Validando conectividade com database..."

max_attempts=30
attempt=1

while [[ $attempt -le $max_attempts ]]; do
    # Usar netcat para verificar conectividade
    if nc -z "$CONEXAO_DE_SORTE_DATABASE_HOST" "$CONEXAO_DE_SORTE_DATABASE_PORT" 2>/dev/null; then
        success "✅ Database está acessível"
        break
    fi

    if [[ $attempt -eq $max_attempts ]]; then
        error "❌ Database não ficou disponível após $max_attempts tentativas"
        exit 1
    fi

    log "⏳ Aguardando database... (tentativa $attempt/$max_attempts)"
    sleep 2
    ((attempt++))
done

# ============================================================================
# 🔴 VALIDAÇÃO DE CONECTIVIDADE - REDIS
# ============================================================================

log "🔍 Validando conectividade com Redis..."

max_attempts=30
attempt=1

while [[ $attempt -le $max_attempts ]]; do
    if nc -z "$CONEXAO_DE_SORTE_REDIS_HOST" "$CONEXAO_DE_SORTE_REDIS_PORT" 2>/dev/null; then
        success "✅ Redis está acessível"
        break
    fi

    if [[ $attempt -eq $max_attempts ]]; then
        error "❌ Redis não ficou disponível após $max_attempts tentativas"
        exit 1
    fi

    log "⏳ Aguardando Redis... (tentativa $attempt/$max_attempts)"
    sleep 2
    ((attempt++))
done

# ============================================================================
# 🐰 VALIDAÇÃO DE CONECTIVIDADE - RABBITMQ (OPCIONAL)
# ============================================================================

if [[ -n "${CONEXAO_DE_SORTE_RABBITMQ_HOST:-}" ]] && [[ -n "${CONEXAO_DE_SORTE_RABBITMQ_PORT:-}" ]]; then
    log "🔍 Validando conectividade com RabbitMQ..."

    max_attempts=15
    attempt=1

    while [[ $attempt -le $max_attempts ]]; do
        if nc -z "$CONEXAO_DE_SORTE_RABBITMQ_HOST" "$CONEXAO_DE_SORTE_RABBITMQ_PORT" 2>/dev/null; then
            success "✅ RabbitMQ está acessível"
            break
        fi

        if [[ $attempt -eq $max_attempts ]]; then
            warning "⚠️ RabbitMQ não ficou disponível após $max_attempts tentativas - continuando sem message queue"
            break
        fi

        log "⏳ Aguardando RabbitMQ... (tentativa $attempt/$max_attempts)"
        sleep 2
        ((attempt++))
    done
else
    log "ℹ️ RabbitMQ não configurado - operando sem message queue"
fi

# ============================================================================
# ⚙️ VALIDAÇÃO DE SCHEDULER HEALTH
# ============================================================================

log "⏰ Validando health checks do Scheduler..."

# Verificar se há configuração de health endpoint
if [[ -n "${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:-}" ]]; then
    if [[ "$MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE" == *"health"* ]]; then
        success "✅ Health endpoint configurado"
    fi
fi

# Verificar se há configuração de metrics
if [[ -n "${MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE:-}" ]]; then
    if [[ "$MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE" == *"metrics"* ]]; then
        log "ℹ️ Metrics endpoint configurado"
    fi
fi

# Validar configuração de job recovery
if [[ -n "${SCHEDULER_RECOVERY_ENABLED:-}" ]]; then
    if [[ "$SCHEDULER_RECOVERY_ENABLED" == "true" ]]; then
        success "✅ Job recovery habilitado"
    fi
fi

# ============================================================================
# 📊 VALIDAÇÃO DE PERFORMANCE
# ============================================================================

log "📊 Validando configurações de performance..."

# Verificar se há configuração de memory limits
if [[ -n "${SCHEDULER_MEMORY_LIMIT:-}" ]]; then
    log "ℹ️ Memory limit configurado: $SCHEDULER_MEMORY_LIMIT"
fi

# Verificar se há configuração de CPU limits
if [[ -n "${SCHEDULER_CPU_LIMIT:-}" ]]; then
    log "ℹ️ CPU limit configurado: $SCHEDULER_CPU_LIMIT"
fi

# Validar configuração de job batch size
if [[ -n "${SCHEDULER_BATCH_SIZE:-}" ]]; then
    log "ℹ️ Batch size configurado: $SCHEDULER_BATCH_SIZE"
fi

# ============================================================================
# 📋 INFORMAÇÕES DO AMBIENTE - SCHEDULER
# ============================================================================

log "📋 Informações do ambiente - Scheduler Microservice:"
echo "  - Service: Conexão de Sorte - Scheduler Microservice"
echo "  - Profile: ${SPRING_PROFILES_ACTIVE:-default}"
echo "  - Server Port: $CONEXAO_DE_SORTE_SERVER_PORT (Padrão: 8084)"
echo "  - Database: $CONEXAO_DE_SORTE_DATABASE_HOST:$CONEXAO_DE_SORTE_DATABASE_PORT"
echo "  - Redis: $CONEXAO_DE_SORTE_REDIS_HOST:$CONEXAO_DE_SORTE_REDIS_PORT"
echo "  - RabbitMQ: ${CONEXAO_DE_SORTE_RABBITMQ_HOST:-Não configurado}:${CONEXAO_DE_SORTE_RABBITMQ_PORT:-5672}"
echo "  - JWT Issuer: $CONEXAO_DE_SORTE_JWT_ISSUER"
echo "  - Max Retry: ${SCHEDULER_MAX_RETRY:-Não configurado}"
echo "  - Job Timeout: ${SCHEDULER_JOB_TIMEOUT:-Não configurado}"
echo "  - Thread Pool: ${SCHEDULER_THREAD_POOL_SIZE:-Não configurado}"
echo "  - Recovery: ${SCHEDULER_RECOVERY_ENABLED:-Não configurado}"
echo "  - Health Endpoint: http://localhost:$CONEXAO_DE_SORTE_SERVER_PORT/actuator/health"

# ============================================================================
# 🏃 EXECUÇÃO DA APLICAÇÃO - SCHEDULER
# ============================================================================

log "🏃 Iniciando Scheduler Microservice..."

# Executar aplicação com exec para permitir signal handling
exec "$@"
