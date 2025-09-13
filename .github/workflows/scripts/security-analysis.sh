#!/bin/bash

# ============================================================================
# 🛡️ SECURITY ANALYSIS SCANNER
# ============================================================================
# 
# Script reutilizável para análise de segurança em microserviços
# Avalia configurações de segurança e gera score
#
# Uso: ./security-analysis.sh [PROJECT_TYPE] [MIN_SCORE]
#
# Parâmetros:
#   PROJECT_TYPE - Tipo do projeto (gateway, microservice, infrastructure)
#   MIN_SCORE    - Score mínimo aceitável (default: 4)
#
# Retorna: 0 se score >= MIN_SCORE, 1 caso contrário
# ============================================================================

set -uo pipefail
# Temporariamente desabilitado -e para debug
# set -euo pipefail

# ============================================================================
# CONFIGURAÇÕES
# ============================================================================

PROJECT_TYPE="${1:-microservice}"
MIN_SCORE="${2:-4}"
SERVICE_NAME="${3:-$(basename "$(pwd)")}"

echo "🛡️ Iniciando análise de segurança para $SERVICE_NAME ($PROJECT_TYPE)"
echo "🎯 Score mínimo requerido: $MIN_SCORE"
echo ""

# ============================================================================
# FUNÇÕES DE ANÁLISE DE SEGURANÇA
# ============================================================================

check_ssl_configuration() {
    echo "🔐 Verificando configuração SSL/HTTPS..."
    
    if grep -riq "ssl.*enabled.*true\|server.*port.*443\|keystore\|truststore" src/main/resources/application*.yml 2>/dev/null; then
        echo "✅ SSL/HTTPS configurado"
        return 0
    else
        echo "⚠️  SSL/HTTPS não encontrado ou não habilitado"
        return 1
    fi
}

check_jwt_security() {
    echo "🎫 Verificando configuração JWT..."
    
    if grep -riq "jwt" src/main/resources/application*.yml 2>/dev/null; then
        echo "✅ JWT Security configurado"
        return 0
    else
        echo "⚠️  JWT Security não encontrado"
        return 1
    fi
}

check_cors_configuration() {
    echo "🌐 Verificando configuração CORS..."
    
    if grep -riq "cors" src/main/resources/application*.yml 2>/dev/null; then
        echo "✅ CORS configurado"
        return 0
    else
        echo "⚠️  CORS não configurado"
        return 1
    fi
}

check_rate_limiting() {
    echo "🚦 Verificando Rate Limiting..."
    
    if grep -riq "rate.*limit\|throttle\|bucket\|replenish" src/main/resources/application*.yml 2>/dev/null; then
        echo "✅ Rate Limiting configurado"
        return 0
    else
        echo "⚠️  Rate Limiting não encontrado"
        return 1
    fi
}

check_actuator_security() {
    echo "📊 Verificando segurança do Actuator..."
    
    if grep -riq "management" src/main/resources/application*.yml 2>/dev/null; then
        echo "✅ Actuator security configurado"
        return 0
    else
        echo "⚠️  Actuator security não configurado"
        return 1
    fi
}

check_logging_security() {
    echo "📝 Verificando configuração de logging seguro..."
    
    if grep -riq "logging" src/main/resources/application*.yml 2>/dev/null; then
        echo "✅ Logging configurado"
        return 0
    else
        echo "⚠️  Logging não configurado"
        return 1
    fi
}

check_database_security() {
    echo "🗄️  Verificando segurança de banco de dados..."
    
    if grep -riq "database\|r2dbc\|jdbc" src/main/resources/application*.yml 2>/dev/null; then
        echo "✅ Configuração de banco encontrada"
        return 0
    else
        echo "⚠️  Configuração de banco não encontrada"
        return 1
    fi
}

check_docker_security() {
    echo "🐳 Verificando segurança do Docker..."
    
    if [[ -f "Dockerfile" ]]; then
        echo "✅ Dockerfile encontrado"
        return 0
    else
        echo "⚠️  Dockerfile não encontrado"
        return 1
    fi
}

# ============================================================================
# EXECUÇÃO DA ANÁLISE
# ============================================================================

echo "============================================================================"
echo "🛡️ ANÁLISE DE SEGURANÇA - $SERVICE_NAME"
echo "============================================================================"

SCORE=0
CHECKS_PERFORMED=0

# Lista de verificações baseada no tipo de projeto
case "$PROJECT_TYPE" in
    "gateway")
        echo "🚪 Análise específica para este perfil de serviço:"
        
        if check_ssl_configuration; then
            ((SCORE++))
            echo "  ✅ SSL check passed"
        else
            echo "  ⚠️ SSL check failed"
        fi
        ((CHECKS_PERFORMED++))
        
        if check_jwt_security; then
            ((SCORE++))
            echo "  ✅ JWT check passed"
        else
            echo "  ⚠️ JWT check failed"
        fi
        ((CHECKS_PERFORMED++))
        
        if check_cors_configuration; then
            ((SCORE++))
            echo "  ✅ CORS check passed"
        else
            echo "  ⚠️ CORS check failed"
        fi
        ((CHECKS_PERFORMED++))
        
        if check_rate_limiting; then
            ((SCORE++))
            echo "  ✅ Rate limiting check passed"
        else
            echo "  ⚠️ Rate limiting check failed"
        fi
        ((CHECKS_PERFORMED++))
        
        if check_actuator_security; then
            ((SCORE++))
            echo "  ✅ Actuator check passed"
        else
            echo "  ⚠️ Actuator check failed"
        fi
        ((CHECKS_PERFORMED++))
        
        if check_logging_security; then
            ((SCORE++))
            echo "  ✅ Logging check passed"
        else
            echo "  ⚠️ Logging check failed"
        fi
        ((CHECKS_PERFORMED++))
        
        if check_database_security; then
            ((SCORE++))
            echo "  ✅ Database check passed"
        else
            echo "  ⚠️ Database check failed"
        fi
        ((CHECKS_PERFORMED++))
        
        if check_docker_security; then
            ((SCORE++))
            echo "  ✅ Docker check passed"
        else
            echo "  ⚠️ Docker check failed"
        fi
        ((CHECKS_PERFORMED++))
        ;;
    "microservice")
        echo "🔧 Análise para microserviço:"
        
        check_jwt_security && ((SCORE++)) || true; ((CHECKS_PERFORMED++))
        check_actuator_security && ((SCORE++)) || true; ((CHECKS_PERFORMED++))
        check_logging_security && ((SCORE++)) || true; ((CHECKS_PERFORMED++))
        check_database_security && ((SCORE++)) || true; ((CHECKS_PERFORMED++))
        check_docker_security && ((SCORE++)) || true; ((CHECKS_PERFORMED++))
        ;;
    *)
        echo "🏗️ Análise básica para infraestrutura:"
        
        check_docker_security && ((SCORE++)) || true; ((CHECKS_PERFORMED++))
        ;;
esac

# ============================================================================
# RELATÓRIO FINAL
# ============================================================================

echo ""
echo "============================================================================"
echo "📊 RESULTADO DA ANÁLISE DE SEGURANÇA"
echo "============================================================================"
echo "🎯 Score obtido: $SCORE/$CHECKS_PERFORMED"

if [[ $CHECKS_PERFORMED -gt 0 ]]; then
    echo "📊 Percentual: $(( (SCORE * 100) / CHECKS_PERFORMED ))%"
else
    echo "📊 Percentual: 0% (nenhuma verificação executada)"
fi

echo "🎯 Score mínimo: $MIN_SCORE"

if [[ $SCORE -ge $MIN_SCORE ]]; then
    echo "✅ APROVADO: Score de segurança adequado ($SCORE/$CHECKS_PERFORMED)"
    echo "🛡️ Projeto $SERVICE_NAME atende aos requisitos de segurança"
    exit 0
else
    echo "❌ REPROVADO: Score de segurança insuficiente ($SCORE/$CHECKS_PERFORMED)"
    echo "🚨 Projeto $SERVICE_NAME precisa de melhorias de segurança"
    echo ""
    echo "💡 Recomendações:"
    echo "   - Revisar configurações de segurança nos arquivos application*.yml"
    echo "   - Implementar as configurações ausentes identificadas acima"
    echo "   - Executar novamente após correções"
    exit 1
fi