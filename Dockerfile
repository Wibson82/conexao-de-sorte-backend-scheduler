# ============================================================================
# 🐳 DOCKERFILE MULTI-ESTÁGIO - MICROSERVIÇO SCHEDULER
# ============================================================================
#
# Dockerfile otimizado para microserviço de scheduler com:
# - Multi-stage build para reduzir tamanho da imagem
# - Java 25 LTS com JVM otimizada para containers
# - Usuário não-root para segurança
# - Health check nativo
# - Otimizações de performance para OAuth2/JWT
# - Suporte a debug remoto (desenvolvimento)
#
# Build: docker build -t conexaodesorte/scheduler:latest .
# Run: docker run -p 8084:8084 conexaodesorte/scheduler:latest
#
# @author Sistema de Migração R2DBC
# @version 1.0
# @since 2024
# ============================================================================

# === ESTÁGIO 1: BUILD ===
FROM amazoncorretto:25-alpine3.22 AS builder

# Instalar Maven
RUN apk add --no-cache maven

# Metadados da imagem
LABEL maintainer="Conexão de Sorte <tech@conexaodesorte.com>"
LABEL description="Microserviço de Autenticação - Build Stage"
LABEL version="1.0.0"

# Variáveis de build
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION=1.0.0

# Definir diretório de trabalho
WORKDIR /build

# Copiar arquivos de configuração Maven (cache layer)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

# Download de dependências (layer cacheável)
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# Copiar código fonte
COPY src/ src/

# Build da aplicação com otimizações
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -B \
    -Dspring-boot.build-image.pullPolicy=IF_NOT_PRESENT \
    -Dmaven.compiler.debug=false \
    -Dmaven.compiler.optimize=true

# === ESTÁGIO 2: RUNTIME ===
FROM amazoncorretto:25-alpine3.22 AS runtime

# Instalar dependências do sistema
RUN apk add --no-cache \
    tzdata \
    curl \
    dumb-init \
    && rm -rf /var/cache/apk/*

# Configurar timezone
ENV TZ=America/Sao_Paulo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Criar usuário não-root para segurança
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Definir diretório da aplicação
WORKDIR /app

# Criar diretório de logs com permissões corretas
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app/logs

# Copiar JAR da aplicação do estágio de build
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Build-time args → ENV
ARG CONEXAO_DE_SORTE_DATABASE_URL
ARG CONEXAO_DE_SORTE_DATABASE_JDBC_URL
ARG CONEXAO_DE_SORTE_DATABASE_R2DBC_URL
ARG CONEXAO_DE_SORTE_DATABASE_USERNAME
ARG CONEXAO_DE_SORTE_DATABASE_PASSWORD
ARG CONEXAO_DE_SORTE_JWT_ISSUER
ARG CONEXAO_DE_SORTE_JWT_JWKS_URI

ENV CONEXAO_DE_SORTE_DATABASE_URL=${CONEXAO_DE_SORTE_DATABASE_URL} \
    CONEXAO_DE_SORTE_DATABASE_JDBC_URL=${CONEXAO_DE_SORTE_DATABASE_JDBC_URL} \
    CONEXAO_DE_SORTE_DATABASE_R2DBC_URL=${CONEXAO_DE_SORTE_DATABASE_R2DBC_URL} \
    CONEXAO_DE_SORTE_DATABASE_USERNAME=${CONEXAO_DE_SORTE_DATABASE_USERNAME} \
    CONEXAO_DE_SORTE_DATABASE_PASSWORD=${CONEXAO_DE_SORTE_DATABASE_PASSWORD} \
    CONEXAO_DE_SORTE_JWT_ISSUER=${CONEXAO_DE_SORTE_JWT_ISSUER} \
    CONEXAO_DE_SORTE_JWT_JWKS_URI=${CONEXAO_DE_SORTE_JWT_JWKS_URI}
## JVM otimizada para containers: flags removidas para compatibilidade total com Java 25 LTS
# As flags e perfis devem ser definidos externamente via workflow/deploy

# Variáveis de ambiente da aplicação devem ser fornecidas externamente (CI/Compose/Helm)
# Definir perfil padrão para container
ENV SPRING_PROFILES_ACTIVE=container

# Expor porta da aplicação


# Health check nativo
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=10 \
  CMD curl -f http://localhost:8084/actuator/health || exit 1

# Copiar script de entrada robusto com retry e healthcheck
COPY --chown=appuser:appgroup docker/healthcheck-entrypoint.sh /app/docker-entrypoint.sh
RUN chmod +x /app/docker-entrypoint.sh

# Mudar para usuário não-root
USER appuser:appgroup

# Labels para metadata
LABEL org.opencontainers.image.title="Conexão de Sorte - Scheduler"
LABEL org.opencontainers.image.description="Microserviço de Scheduler ETL para Loterias"
LABEL org.opencontainers.image.version=${VERSION}
LABEL org.opencontainers.image.created=${BUILD_DATE}
LABEL org.opencontainers.image.revision=${VCS_REF}
LABEL org.opencontainers.image.vendor="Conexão de Sorte"
LABEL org.opencontainers.image.licenses="MIT"
LABEL org.opencontainers.image.url="https://conexaodesorte.com"
LABEL org.opencontainers.image.source="https://github.com/conexaodesorte/autenticacao"

# Comando de inicialização com pré-checagem de DB e retry
ENTRYPOINT ["/app/docker-entrypoint.sh"]

# === ESTÁGIO 3: DEBUG (Opcional) ===
FROM runtime AS debug

# Configurar debug remoto
ENV JAVA_OPTS="$JAVA_OPTS \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
    -Dspring.profiles.active=dev \
    -Dlogging.level.br.tec.facilitaservicos=DEBUG"

# Expor porta de debug


# Comando para debug com retry
ENTRYPOINT ["/app/docker-entrypoint.sh", "debug"]
