#!/bin/sh
# ============================================================================
# 🚀 SCRIPT DE ENTRADA PARA CONTAINER SIMPLIFICADO (SWARM COMPATÍVEL)
# ============================================================================
#
# Este script inicia a aplicação Java.
# A descoberta de serviços e a resiliência são gerenciadas pelo Docker Swarm.
#
# @version 2.0
# @since 2025-09-21
# ============================================================================

set -e

# Função para logging com timestamp
log() {
    echo "$(date '+%Y-%m-%dT%H:%M:%S%z') $*"
}

log "🚀 Iniciando aplicação Java..."

# Executa a aplicação Java
if [ "$1" = "debug" ]; then
    log "🐛 Iniciando em modo DEBUG na porta 5005"
    java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
         -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-dev} \
         -Dlogging.level.br.tec.facilitaservicos=DEBUG \
         -jar /app/app.jar
else
    log "🚀 Iniciando aplicação em modo normal"
    # Define perfil baseado na variável de ambiente ou padrão para prod
    PROFILE=${SPRING_PROFILES_ACTIVE:-prod}
    log "📋 Perfil ativo: $PROFILE"

    java -Dspring.profiles.active=$PROFILE \
         -Xmx2g \
         -XX:+UseG1GC \
         -XX:MaxGCPauseMillis=200 \
         -jar /app/app.jar
fi

# O script deve sair com o código de saída da aplicação Java
# O Docker Swarm e o healthcheck do Dockerfile/docker-compose.yml
# irão gerenciar a resiliência e o restart em caso de falha.