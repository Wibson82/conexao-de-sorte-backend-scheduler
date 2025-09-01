#!/bin/sh
set -eu

log() {
  printf '%s %s\n' "$(date '+%Y-%m-%dT%H:%M:%S%z')" "$*"
}

SECRETS_DIR=${SECRETS_DIR:-/run/secrets}
R2DBC_FILE="$SECRETS_DIR/spring.r2dbc.url"
JDBC_FILE="$SECRETS_DIR/spring.flyway.url"

has_nc() {
  command -v nc >/dev/null 2>&1
}

can_connect() {
  host="$1"; port="$2"
  if has_nc; then
    log "→ Testando TCP $host:$port (nc)"
    nc -z -w 2 "$host" "$port" >/dev/null 2>&1
  else
    # Fallback best-effort using /dev/tcp
    log "→ Testando TCP $host:$port (/dev/tcp)"
    (echo > /dev/tcp/"$host"/"$port") >/dev/null 2>&1 || return 1
  fi
}

rewrite_urls() {
  new_hostport="$1"
  log "⤴️  Reescrevendo URLs para host:porta '$new_hostport'"
  # Update R2DBC
  if [ -f "$R2DBC_FILE" ]; then
    r2dbc=$(cat "$R2DBC_FILE")
    proto="r2dbc:mysql://"
    rest="${r2dbc#${proto}}"
    rest_no_host="${rest#*/}"
    echo "${proto}${new_hostport}/${rest_no_host}" > "$R2DBC_FILE"
    log "R2DBC URL -> $(cat "$R2DBC_FILE")"
  fi
  # Update JDBC
  if [ -f "$JDBC_FILE" ]; then
    jdbc=$(cat "$JDBC_FILE")
    proto="jdbc:mysql://"
    rest="${jdbc#${proto}}"
    rest_no_host="${rest#*/}"
    echo "${proto}${new_hostport}/${rest_no_host}" > "$JDBC_FILE"
    log "JDBC URL  -> $(cat "$JDBC_FILE")"
  fi
}

preflight_db() {
  [ -f "$R2DBC_FILE" ] || return 0
  url=$(cat "$R2DBC_FILE")
  log "🔎 URL R2DBC atual: $url"
  base="${url#r2dbc:mysql://}"
  hostport="${base%%/*}"
  host="${hostport%%:*}"
  port="${hostport#*:}"
  [ "$port" = "$host" ] && port=3306
  log "🔎 Host alvo: $host | Porta: $port"

  # Coletar informações de rede
  gw=$(ip route 2>/dev/null | awk '/default/ {print $3; exit}')
  [ -z "${gw:-}" ] && gw=$(awk '$2==00000000 {print $3}' /proc/net/route 2>/dev/null | head -n1)
  log "🌐 Gateway padrão: ${gw:-desconhecido}"
  if command -v ip >/dev/null 2>&1; then
    log "🌐 Interfaces:"
    ip -o -4 addr show | awk '{print "    "$2" -> "$4}' | while read -r line; do log "$line"; done
  fi

  # 1) Try current
  if can_connect "$host" "$port"; then
    log "✅ DB alcançável em $host:$port"
    return 0
  fi

  log "⚠️ DB inacessível em $host:$port. Tentando fallbacks..."

  # 2) conexao-mysql (mesma rede)
  if can_connect "conexao-mysql" "$port"; then
    log "🔁 Alternando para conexao-mysql:$port"
    rewrite_urls "conexao-mysql:$port"
    return 0
  fi

  # 3) host.docker.internal
  if can_connect "host.docker.internal" "$port"; then
    log "🔁 Alternando para host.docker.internal:$port"
    rewrite_urls "host.docker.internal:$port"
    return 0
  fi

  # 4) Docker gateway
  if [ -n "${gw:-}" ] && can_connect "$gw" "$port"; then
    log "🔁 Alternando para gateway $gw:$port"
    rewrite_urls "$gw:$port"
    return 0
  fi

  # 5) localhost (último recurso solicitado)
  if can_connect "127.0.0.1" "$port" || can_connect "localhost" "$port"; then
    log "🔁 Alternando para localhost:$port"
    rewrite_urls "127.0.0.1:$port"
    return 0
  fi

  log "❌ Nenhum host de DB acessível a partir do container. Prosseguindo; a aplicação pode falhar."
}

preflight_db || true

log "🚀 Iniciando aplicação Java"
exec dumb-init -- java -jar /app/app.jar
