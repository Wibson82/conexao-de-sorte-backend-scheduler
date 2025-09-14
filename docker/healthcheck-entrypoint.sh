#!/bin/sh
# ============================================================================
# 🚀 SCRIPT DE ENTRADA PARA CONTAINER COM HEALTHCHECK E RETRY LIMITADO
# ============================================================================
#
# Este script implementa:
# - Verificação de conexão com banco de dados
# - Tentativas limitadas de inicialização (máx 10)
# - Tempo de espera entre tentativas (30 segundos)
# - Logs detalhados de diagnóstico
#
# @version 1.0
# @since 2025-09-14
# ============================================================================

set -e

# Variáveis de configuração
MAX_RETRIES=10
RETRY_INTERVAL=30
RETRY_COUNT=0
SECRETS_DIR=${SECRETS_DIR:-/run/secrets}
R2DBC_FILE="$SECRETS_DIR/spring.r2dbc.url"
JDBC_FILE="$SECRETS_DIR/spring.flyway.url"

# Função para logging com timestamp
log() {
    echo "$(date '+%Y-%m-%dT%H:%M:%S%z') $*"
}

# Verifica se o comando nc está disponível
has_nc() {
    command -v nc >/dev/null 2>&1;
}

# Tenta conectar em um host:porta
can_connect() {
    host="$1"
    port="$2"
    if has_nc; then
        nc -z -w 2 "$host" "$port" >/dev/null 2>&1
    else
        (echo > /dev/tcp/"$host"/"$port") >/dev/null 2>&1 || return 1
    fi
}

# Reescreve as URLs de conexão com banco
rewrite_urls() {
    new_hostport="$1"
    if [ -f "$R2DBC_FILE" ]; then
        r2dbc=$(cat "$R2DBC_FILE")
        proto="r2dbc:mysql://"
        rest="${r2dbc#${proto}}"
        rest_no_host="${rest#*/}"
        echo "${proto}${new_hostport}/${rest_no_host}" > "$R2DBC_FILE"
    fi

    if [ -f "$JDBC_FILE" ]; then
        jdbc=$(cat "$JDBC_FILE")
        proto="jdbc:mysql://"
        rest="${jdbc#${proto}}"
        rest_no_host="${rest#*/}"
        echo "${proto}${new_hostport}/${rest_no_host}" > "$JDBC_FILE"
    fi
}

# Verificação de conectividade com banco
preflight_db() {
    [ -f "$R2DBC_FILE" ] || return 0

    url=$(cat "$R2DBC_FILE")
    base="${url#r2dbc:mysql://}"
    hostport="${base%%/*}"
    host="${hostport%%:*}"
    port="${hostport#*:}"
    [ "$port" = "$host" ] && port=3306

    log "🔍 Verificando conexão com banco de dados em $host:$port..."

    if can_connect "$host" "$port"; then
        log "✅ Conexão bem-sucedida com banco de dados em $host:$port"
        return 0
    fi

    log "⚠️ Não foi possível conectar ao banco em $host:$port, tentando alternativas..."

    for alt in "conexao-mysql" "scheduler-mysql" "mysql-db" "host.docker.internal"; do
        log "🔄 Tentando alternativa: $alt:$port"
        if can_connect "$alt" "$port"; then
            log "✅ Conexão alternativa bem-sucedida em $alt:$port"
            rewrite_urls "$alt:$port"
            return 0
        fi
    done

    gw=$(ip route 2>/dev/null | awk "/default/ {print $3; exit}")
    if [ -n "${gw:-}" ] && can_connect "$gw" "$port"; then
        log "✅ Conexão via gateway bem-sucedida em $gw:$port"
        rewrite_urls "$gw:$port"
        return 0
    fi

    if can_connect 127.0.0.1 "$port" || can_connect localhost "$port"; then
        log "✅ Conexão via localhost bem-sucedida"
        rewrite_urls "127.0.0.1:$port"
        return 0
    fi

    log "❌ Não foi possível estabelecer conexão com o banco de dados após tentar múltiplas alternativas"
    return 1
}

# Loop de tentativas com limite
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    # Incrementa contador
    RETRY_COUNT=$((RETRY_COUNT + 1))

    # Mostra tentativa atual
    log "🚀 Tentativa $RETRY_COUNT/$MAX_RETRIES de iniciar o serviço Scheduler"

    # Verificação de pré-requisitos
    preflight_db

    # Executa a aplicação
    if [ "$1" = "debug" ]; then
        log "🐛 Iniciando em modo DEBUG na porta 5005"
        java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
             -Dspring.profiles.active=dev \
             -Dlogging.level.br.tec.facilitaservicos=DEBUG \
             -jar /app/app.jar
    else
        log "🚀 Iniciando aplicação em modo normal"
        java -jar /app/app.jar
    fi

    # Verifica resultado
    RESULT=$?

    # Se aplicação saiu com sucesso ou foi interrompida pelo usuário, sai do loop
    if [ $RESULT -eq 0 ] || [ $RESULT -eq 130 ] || [ $RESULT -eq 143 ]; then
        log "✅ Aplicação encerrada normalmente com código $RESULT"
        exit $RESULT
    fi

    # Se atingiu o número máximo de tentativas, termina com erro
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        log "❌ Número máximo de tentativas atingido ($MAX_RETRIES). Encerrando com erro."
        exit 1
    fi

    # Aguarda antes da próxima tentativa
    log "⏳ Aguardando $RETRY_INTERVAL segundos antes da próxima tentativa..."
    sleep $RETRY_INTERVAL
done