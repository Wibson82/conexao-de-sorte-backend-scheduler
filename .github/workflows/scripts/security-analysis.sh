#!/bin/bash

# ============================================================================
# 🛡️ SECURITY ANALYSIS SCANNER
# ============================================================================
# Script reutilizável para análise de segurança em microsserviços
# Avalia configurações de segurança e gera score
# ============================================================================

set -euo pipefail

PROJECT_TYPE="${1:-microservice}"
MIN_SCORE="${2:-4}"
SERVICE_NAME="${3:-$(basename "$(pwd)")}"

echo "🛡️ Iniciando análise de segurança para $SERVICE_NAME ($PROJECT_TYPE)"
echo "🎯 Score mínimo requerido: $MIN_SCORE"
echo ""

score=0

# Check 1: Dockerfile security
if [[ -f "Dockerfile" ]]; then
    if grep -q "USER.*[0-9]" Dockerfile; then
        echo "✅ Non-root user configured in Dockerfile"
        ((score++))
    else
        echo "❌ No non-root user found in Dockerfile"
    fi
    
    if grep -q "HEALTHCHECK" Dockerfile; then
        echo "✅ Health check configured"
        ((score++))
    else
        echo "⚠️ No health check found"
    fi
else
    echo "❌ Dockerfile not found"
fi

# Check 2: Docker Compose security
if [[ -f "docker-compose.yml" ]]; then
    if grep -q "secrets:" docker-compose.yml; then
        echo "✅ Docker secrets configured"
        ((score++))
    else
        echo "⚠️ No Docker secrets found"
    fi
    
    if ! grep -q "password.*:" docker-compose.yml | grep -v "\${" | grep -v "external:"; then
        echo "✅ No hardcoded passwords in docker-compose.yml"
        ((score++))
    else
        echo "❌ Potential hardcoded passwords found"
    fi
else
    echo "❌ docker-compose.yml not found"
fi

# Check 3: Application configuration
if [[ -f "src/main/resources/application.yml" ]]; then
    if grep -q "oauth2:" src/main/resources/application.yml; then
        echo "✅ OAuth2 security configured"
        ((score++))
    else
        echo "⚠️ No OAuth2 configuration found"
    fi
else
    echo "⚠️ application.yml not found"
fi

echo ""
echo "🏆 Security Score: $score/5"

if [[ $score -ge $MIN_SCORE ]]; then
    echo "✅ Security validation passed (score: $score >= $MIN_SCORE)"
    exit 0
else
    echo "❌ Security validation failed (score: $score < $MIN_SCORE)"
    exit 1
fi
