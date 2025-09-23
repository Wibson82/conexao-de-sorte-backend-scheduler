#!/bin/bash
set -e

echo "🗄️ Inicializando database para conexao-sorte-scheduler..."

# Obter configurações dos secrets
DB_URL_SECRET=$(cat /run/secrets/conexao-de-sorte-database-r2dbc-url)
DB_USERNAME=$(cat /run/secrets/conexao-de-sorte-database-username)
DB_PASSWORD=$(cat /run/secrets/conexao-de-sorte-database-password)

# Extrair host e porta da URL R2DBC
# Formato: r2dbc:mysql://host:port
DB_HOST_PORT=$(echo "$DB_URL_SECRET" | sed 's|r2dbc:mysql://||' | cut -d'/' -f1)
DB_HOST=$(echo "$DB_HOST_PORT" | cut -d':' -f1)
DB_PORT=$(echo "$DB_HOST_PORT" | cut -d':' -f2)

echo "📍 Conectando ao MySQL em $DB_HOST:$DB_PORT"

# Aguardar MySQL estar disponível
echo "⏳ Aguardando MySQL estar disponível..."
for i in {1..30}; do
    if nc -z "$DB_HOST" "$DB_PORT"; then
        echo "✅ MySQL está disponível"
        break
    fi
    echo "⏳ Tentativa $i/30 - aguardando MySQL..."
    sleep 2
done

# Verificar se conseguiu conectar
if ! nc -z "$DB_HOST" "$DB_PORT"; then
    echo "❌ Erro: Não foi possível conectar ao MySQL em $DB_HOST:$DB_PORT"
    exit 1
fi

# Criar database se não existir
echo "🔧 Criando database conexao_sorte_scheduler se não existir..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USERNAME" -p"$DB_PASSWORD" -e "
CREATE DATABASE IF NOT EXISTS conexao_sorte_scheduler
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
" || {
    echo "❌ Erro ao criar database"
    exit 1
}

echo "✅ Database conexao_sorte_scheduler criado/verificado com sucesso"
echo "🚀 Iniciando aplicação..."