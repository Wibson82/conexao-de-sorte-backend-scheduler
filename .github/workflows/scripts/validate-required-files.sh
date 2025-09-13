#!/bin/bash

# ============================================================================
# 📁 REQUIRED FILES VALIDATOR
# ============================================================================
# 
# Script reutilizável para validar arquivos obrigatórios em microserviços
# Pode ser customizado para diferentes tipos de projeto
#
# Uso: ./validate-required-files.sh [PROJECT_TYPE]
#
# PROJECT_TYPE suportados:
#   - gateway: Validação específica para Gateway
#   - microservice: Validação padrão para microserviços
#   - infrastructure: Validação para projetos de infraestrutura
#   - frontend: Validação para projetos frontend
#
# Retorna: 0 se todos os arquivos existem, 1 se algum está ausente
# ============================================================================

set -euo pipefail

# ============================================================================
# CONFIGURAÇÕES
# ============================================================================

PROJECT_TYPE="${1:-microservice}"
SERVICE_NAME="${2:-$(basename "$(pwd)")}"

echo "🔍 Validando arquivos obrigatórios para projeto tipo: $PROJECT_TYPE"
echo "📦 Serviço: $SERVICE_NAME"
echo ""

# ============================================================================
# LISTAS DE ARQUIVOS POR TIPO DE PROJETO
# ============================================================================

declare -a REQUIRED_FILES
declare -a OPTIONAL_FILES

case "$PROJECT_TYPE" in
    "gateway")
        REQUIRED_FILES=(
            "Dockerfile"
            "docker-compose.yml"
            "pom.xml"
            "src/main/resources/application.yml"
            "src/main/resources/application-azure.yml"
            "src/main/java"
        )
        OPTIONAL_FILES=(
            "src/main/resources/application-prod.yml"
            "src/main/resources/application-dev.yml"
            "src/main/resources/logback-spring.xml"
            ".env"
        )
        ;;
    "microservice")
        REQUIRED_FILES=(
            "Dockerfile"
            "docker-compose.yml"
            "pom.xml"
            "src/main/resources/application.yml"
            "src/main/resources/application-azure.yml"
            "src/main/java"
        )
        OPTIONAL_FILES=(
            "src/main/resources/application-prod.yml"
            "src/main/resources/logback-spring.xml"
        )
        ;;
    "infrastructure")
        REQUIRED_FILES=(
            "docker-compose.yml"
            "Dockerfile"
        )
        OPTIONAL_FILES=(
            "docker-compose.prod.yml"
            "scripts/"
            ".env"
        )
        ;;
    "frontend")
        REQUIRED_FILES=(
            "package.json"
            "Dockerfile"
            "src/"
        )
        OPTIONAL_FILES=(
            "docker-compose.yml"
            ".env"
            "public/"
        )
        ;;
    *)
        echo "❌ Tipo de projeto não suportado: $PROJECT_TYPE"
        echo "Tipos suportados: gateway, microservice, infrastructure, frontend"
        exit 1
        ;;
esac

# ============================================================================
# FUNÇÃO DE VALIDAÇÃO
# ============================================================================

validate_files() {
    local file_type="$1"
    shift
    local -a files_to_check=("$@")
    local missing_count=0
    local found_count=0
    
    echo "📋 Verificando arquivos $file_type:"
    
    for file in "${files_to_check[@]}"; do
        if [[ -f "$file" || -d "$file" ]]; then
            echo "✅ $file"
            ((found_count++))
        else
            if [[ "$file_type" == "obrigatórios" ]]; then
                echo "❌ $file (AUSENTE)"
                ((missing_count++))
            else
                echo "⚠️  $file (opcional - ausente)"
            fi
        fi
    done
    
    echo ""
    return $missing_count
}

# ============================================================================
# VALIDAÇÕES ESPECÍFICAS
# ============================================================================

validate_yaml_syntax() {
    echo "🔍 Validando sintaxe YAML..."
    
    for file in src/main/resources/application*.yml; do
        if [[ -f "$file" ]]; then
            if python3 -c "
import yaml
import sys
try:
    with open('$file', 'r') as f:
        list(yaml.safe_load_all(f))
    print('✅ $file: Sintaxe YAML válida')
except Exception as e:
    print(f'❌ $file: Erro YAML - {e}')
    sys.exit(1)
" 2>/dev/null; then
                continue
            else
                echo "❌ $file: Erro de sintaxe YAML"
                return 1
            fi
        fi
    done
    echo ""
}

validate_docker_compose() {
    if [[ -f "docker-compose.yml" ]]; then
        echo "🐳 Validando Docker Compose..."
        
        # Criar .env temporário se não existir (para validação)
        local temp_env_created=false
        if [[ ! -f ".env" ]]; then
            cat > .env << 'EOF'
# Arquivo temporário para validação
REDIS_PASSWORD=temp
DATABASE_PASSWORD=temp
JWT_SECRET=temp
EOF
            temp_env_created=true
        fi
        
        if docker compose config >/dev/null 2>&1; then
            echo "✅ docker-compose.yml: Sintaxe válida"
        else
            echo "❌ docker-compose.yml: Erro de configuração"
            return 1
        fi
        
        # Limpar .env temporário
        if [[ "$temp_env_created" == "true" ]]; then
            rm -f .env
        fi
        echo ""
    fi
}

validate_maven_project() {
    if [[ -f "pom.xml" ]]; then
        echo "☕ Validando projeto Maven..."
        
        if ./mvnw help:effective-pom -q >/dev/null 2>&1; then
            echo "✅ pom.xml: Configuração Maven válida"
        else
            echo "❌ pom.xml: Erro na configuração Maven"
            return 1
        fi
        echo ""
    fi
}

# ============================================================================
# EXECUÇÃO DAS VALIDAÇÕES
# ============================================================================

echo "============================================================================"
echo "📁 VALIDADOR DE ARQUIVOS OBRIGATÓRIOS"
echo "============================================================================"

# Validar arquivos obrigatórios
if ! validate_files "obrigatórios" "${REQUIRED_FILES[@]}"; then
    echo "🚨 VALIDAÇÃO FALHOU: Arquivos obrigatórios ausentes"
    exit 1
fi

# Validar arquivos opcionais (só para info)
validate_files "opcionais" "${OPTIONAL_FILES[@]}" || true

# Validações específicas por tipo de projeto
case "$PROJECT_TYPE" in
    "gateway"|"microservice")
        validate_yaml_syntax || exit 1
        validate_docker_compose || exit 1
        validate_maven_project || exit 1
        ;;
    "infrastructure")
        validate_docker_compose || exit 1
        ;;
esac

echo "============================================================================"
echo "🎉 VALIDAÇÃO COMPLETA: Todos os arquivos obrigatórios estão presentes"
echo "📦 Projeto $SERVICE_NAME ($PROJECT_TYPE) pronto para build"
echo "============================================================================"

exit 0