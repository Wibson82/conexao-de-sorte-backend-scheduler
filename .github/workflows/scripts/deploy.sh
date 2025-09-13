#!/bin/bash

# ============================================================================
# 🚀 SCRIPT DE DEPLOY SIMPLIFICADO - CONEXÃO DE SORTE SCHEDULER
# ============================================================================
#
# Script para facilitar o deploy manual e validar o ambiente
# Pode ser usado tanto localmente quanto nos servidores de deploy
#
# Uso: ./deploy.sh [staging|production] [image_tag]
#
# Parâmetros:
#   ENVIRONMENT  - staging ou production (obrigatório)
#   IMAGE_TAG    - Tag da imagem Docker (opcional, padrão: latest)
#
# ============================================================================

set -euo pipefail

# Configurações
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="/opt/conexao-de-sorte/scheduler"
SERVICE_NAME="scheduler"
COMPOSE_FILE="docker-compose.yml"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Função de log com cores
log() {
    local level=$1
    local message=$2
    case $level in
        ERROR)
            echo -e "${RED}❌ ERRO: $message${NC}"
            ;;
        SUCCESS)
            echo -e "${GREEN}✅ $message${NC}"
            ;;
        WARNING)
            echo -e "${YELLOW}⚠️ AVISO: $message${NC}"
            ;;
        INFO)
            echo -e "${BLUE}ℹ️ $message${NC}"
            ;;
        *)
            echo "$message"
            ;;
    esac
}

# Função para verificar pré-requisitos
check_prerequisites() {
    log "INFO" "Verificando pré-requisitos..."

    # Verificar Docker
    if ! command -v docker &> /dev/null; then
        log "ERROR" "Docker não está instalado"
        exit 1
    fi

    # Verificar Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log "ERROR" "Docker Compose não está instalado"
        exit 1
    fi

    # Verificar se Docker está rodando
    if ! docker info &> /dev/null; then
        log "ERROR" "Docker não está rodando"
        exit 1
    fi

    log "SUCCESS" "Todos os pré-requisitos foram atendidos"
}

# Função para validar parâmetros
validate_params() {
    if [[ $# -lt 1 ]]; then
        log "ERROR" "Ambiente é obrigatório"
        echo "Uso: $0 [staging|production] [image_tag]"
        exit 1
    fi

    ENVIRONMENT=$1
    IMAGE_TAG=${2:-"latest"}

    if [[ "$ENVIRONMENT" != "staging" && "$ENVIRONMENT" != "production" ]]; then
        log "ERROR" "Ambiente deve ser 'staging' ou 'production'"
        exit 1
    fi

    log "INFO" "Deploy configurado para: $ENVIRONMENT"
    log "INFO" "Imagem Docker: $IMAGE_TAG"
}

# Função para criar backup
create_backup() {
    log "INFO" "Criando backup da configuração atual..."

    local backup_dir="$PROJECT_DIR/backups"
    local backup_file="$backup_dir/backup-$(date +%Y%m%d_%H%M%S).tar.gz"

    mkdir -p "$backup_dir"

    # Backup dos logs de containers em execução
    if docker-compose ps -q &> /dev/null; then
        docker-compose logs > "$backup_dir/containers-$(date +%Y%m%d_%H%M%S).log" 2>&1 || true
    fi

    # Backup das configurações
    tar -czf "$backup_file" \
        --exclude="$backup_dir" \
        --exclude="logs" \
        -C "$PROJECT_DIR" . 2>/dev/null || true

    log "SUCCESS" "Backup criado: $backup_file"
}

# Função para verificar saúde dos serviços
health_check() {
    log "INFO" "Verificando saúde dos serviços..."

    local max_attempts=30
    local attempt=1

    while [[ $attempt -le $max_attempts ]]; do
        log "INFO" "Tentativa $attempt/$max_attempts..."

        # Verificar se todos os containers estão rodando
        local running_containers=$(docker-compose ps -q | wc -l)
        local healthy_containers=$(docker-compose ps | grep -c "Up" || echo "0")

        if [[ $running_containers -gt 0 && $healthy_containers -eq $running_containers ]]; then
            log "SUCCESS" "Todos os serviços estão saudáveis!"

            # Mostrar status dos containers
            echo ""
            log "INFO" "Status atual dos containers:"
            docker-compose ps

            return 0
        fi

        log "WARNING" "Aguardando serviços ficarem saudáveis... ($attempt/$max_attempts)"
        sleep 10
        ((attempt++))
    done

    log "ERROR" "Timeout: serviços não ficaram saudáveis em tempo hábil"
    docker-compose ps
    docker-compose logs --tail=50
    return 1
}

# Função principal de deploy
deploy() {
    log "INFO" "Iniciando deploy para ambiente: $ENVIRONMENT"

    # Navegar para diretório do projeto
    if [[ ! -d "$PROJECT_DIR" ]]; then
        log "WARNING" "Diretório do projeto não existe, criando: $PROJECT_DIR"
        sudo mkdir -p "$PROJECT_DIR"
        cd "$PROJECT_DIR"
    else
        cd "$PROJECT_DIR"
    fi

    # Verificar se existe docker-compose.yml
    if [[ ! -f "$COMPOSE_FILE" ]]; then
        log "WARNING" "Arquivo $COMPOSE_FILE não encontrado"
        log "INFO" "Criando arquivo docker-compose.yml básico..."

        # Criar um docker-compose.yml básico se não existir
        cat > "$COMPOSE_FILE" << EOF
version: '3.8'

services:
  scheduler:
    image: ghcr.io/\${GITHUB_REPOSITORY:-conexao-de-sorte/conexao-de-sorte-backend-scheduler}-scheduler:\${IMAGE_TAG}
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=\${ENVIRONMENT}
      - TZ=America/Sao_Paulo
    secrets:
      - JWT_PRIVATE_KEY
      - JWT_PUBLIC_KEY
      - JWT_SIGNING_KEY
      - JWT_VERIFICATION_KEY
      - JWT_SECRET
      - JWT_KEY_ID
      - JWT_ISSUER
      - JWT_JWKS_URI
      - REDIS_HOST
      - REDIS_PORT
      - REDIS_PASSWORD
      - REDIS_DATABASE
      - DATABASE_R2DBC_URL
      - DATABASE_USERNAME
      - DATABASE_PASSWORD
      - CORS_ALLOWED_ORIGINS
      - CORS_ALLOW_CREDENTIALS
    networks:
      - app-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    deploy:
      resources:
        limits:
          memory: 1G
        reservations:
          memory: 512M

secrets:
  JWT_PRIVATE_KEY:
    external: true
  JWT_PUBLIC_KEY:
    external: true
  JWT_SIGNING_KEY:
    external: true
  JWT_VERIFICATION_KEY:
    external: true
  JWT_SECRET:
    external: true
  JWT_KEY_ID:
    external: true
  JWT_ISSUER:
    external: true
  JWT_JWKS_URI:
    external: true
  REDIS_HOST:
    external: true
  REDIS_PORT:
    external: true
  REDIS_PASSWORD:
    external: true
  REDIS_DATABASE:
    external: true
  DATABASE_R2DBC_URL:
    external: true
  DATABASE_USERNAME:
    external: true
  DATABASE_PASSWORD:
    external: true
  CORS_ALLOWED_ORIGINS:
    external: true
  CORS_ALLOW_CREDENTIALS:
    external: true

networks:
  app-network:
    driver: bridge
EOF
        log "SUCCESS" "Arquivo docker-compose.yml criado"
    fi

    # Criar backup se em produção
    if [[ "$ENVIRONMENT" == "production" ]]; then
        create_backup
    fi

    # Atualizar variáveis de ambiente no compose file
    export IMAGE_TAG
    export ENVIRONMENT

    log "INFO" "Baixando imagem Docker..."
    docker-compose pull

    log "INFO" "Parando serviços antigos..."
    docker-compose down || true

    log "INFO" "Iniciando novos serviços..."
    docker-compose up -d

    # Verificar saúde
    if health_check; then
        log "SUCCESS" "Deploy concluído com sucesso!"
    else
        log "ERROR" "Deploy falhou na verificação de saúde"

        log "INFO" "Tentando rollback..."
        docker-compose down

        # Se há backup, tentar restaurar
        if [[ "$ENVIRONMENT" == "production" ]]; then
            log "WARNING" "Considere restaurar o backup manual se necessário"
        fi

        exit 1
    fi

    # Limpeza de imagens antigas
    log "INFO" "Limpando imagens Docker antigas..."
    docker image prune -f || true

    log "SUCCESS" "Deploy finalizado com sucesso!"
}

# Função para mostrar logs
show_logs() {
    log "INFO" "Mostrando logs dos últimos 100 linhas..."
    docker-compose logs --tail=100 -f
}

# Função para rollback
rollback() {
    log "WARNING" "Iniciando rollback..."

    cd "$PROJECT_DIR"

    # Parar serviços atuais
    docker-compose down

    # Buscar último backup
    local latest_backup=$(ls -t backups/backup-*.tar.gz 2>/dev/null | head -n1)

    if [[ -n "$latest_backup" ]]; then
        log "INFO" "Restaurando backup: $latest_backup"
        tar -xzf "$latest_backup" -C "$PROJECT_DIR"
        docker-compose up -d

        if health_check; then
            log "SUCCESS" "Rollback concluído com sucesso!"
        else
            log "ERROR" "Rollback falhou!"
            exit 1
        fi
    else
        log "ERROR" "Nenhum backup encontrado para rollback"
        exit 1
    fi
}

# Menu principal
main() {
    case "${1:-}" in
        "deploy")
            validate_params "${@:2}"
            check_prerequisites
            deploy
            ;;
        "logs")
            show_logs
            ;;
        "rollback")
            rollback
            ;;
        "health")
            health_check
            ;;
        *)
            echo "Uso: $0 {deploy|logs|rollback|health} [parâmetros]"
            echo ""
            echo "Comandos:"
            echo "  deploy [staging|production] [image_tag]  - Executa deploy"
            echo "  logs                                     - Mostra logs em tempo real"
            echo "  rollback                                 - Executa rollback para último backup"
            echo "  health                                   - Verifica saúde dos serviços"
            echo ""
            echo "Exemplos:"
            echo "  $0 deploy staging latest"
            echo "  $0 deploy production v1.2.3"
            echo "  $0 logs"
            echo "  $0 health"
            exit 1
            ;;
    esac
}

# Executar função principal
main "$@"
