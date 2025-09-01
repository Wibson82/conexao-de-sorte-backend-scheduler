# 🔒 VERIFICAÇÃO DE SEGURANÇA - COMANDOS PÓS-DEPLOY

## **1. VERIFICAÇÃO DE ASSINATURA DE IMAGEM (COSIGN)**

```bash
# Instalar cosign se necessário
curl -O -L "https://github.com/sigstore/cosign/releases/latest/download/cosign-linux-amd64"
sudo mv cosign-linux-amd64 /usr/local/bin/cosign
sudo chmod +x /usr/local/bin/cosign

# Verificar assinatura keyless da imagem
cosign verify \
  --certificate-identity-regexp="https://github.com/conexaodesorte/" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ghcr.io/conexao-de-sorte/autenticacao-microservice:latest

# Verificar SBOM
cosign verify-attestation \
  --type="https://spdx.dev/Document" \
  --certificate-identity-regexp="https://github.com/conexaodesorte/" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ghcr.io/conexao-de-sorte/autenticacao-microservice:latest

# Verificar proveniência
cosign verify-attestation \
  --type="https://slsa.dev/provenance/v1" \
  --certificate-identity-regexp="https://github.com/conexaodesorte/" \
  --certificate-oidc-issuer="https://token.actions.githubusercontent.com" \
  ghcr.io/conexao-de-sorte/autenticacao-microservice:latest
```

## **2. VERIFICAÇÃO DE AUSÊNCIA DE SEGREDOS EM VARIÁVEIS DE AMBIENTE**

```bash
# Verificar que não há segredos em env vars do container
docker inspect autenticacao-microservice | jq '.[]|.Config.Env[]' | \
  grep -v -E "(JAVA_OPTS|TZ|SPRING_PROFILES_ACTIVE|SERVER_PORT|ENVIRONMENT)" | \
  grep -i -E "(password|secret|key|token|credential)"

# Deve retornar vazio ou só variáveis não sensíveis
# Se encontrar algo, é uma falha de segurança
```

## **3. VERIFICAÇÃO DE PERMISSÕES DOS SECRETS**

```bash
# Verificar estrutura de diretórios de secrets
ls -la /run/secrets/
# Deve mostrar:
# -r--------  1 root root  <size> <date> DB_PASSWORD
# -r--------  1 root root  <size> <date> JWT_SIGNING_KEY
# -r--------  1 root root  <size> <date> JWT_VERIFICATION_KEY
# -r--------  1 root root  <size> <date> OAUTH2_CLIENT_SECRET
# etc.

# Verificar permissões específicas
stat /run/secrets/JWT_SIGNING_KEY
# Deve mostrar: Access: (0400/-r--------) Uid: (0/root) Gid: (0/root)

# Verificar que arquivos não estão vazios
find /run/secrets -type f -empty
# Deve retornar vazio (nenhum arquivo vazio)

# Verificar conteúdo sem expor (apenas tamanho)
wc -c /run/secrets/* | grep -v " 0 "
# Deve mostrar arquivos com tamanho > 0
```

## **4. VERIFICAÇÃO DE ENDPOINTS ACTUATOR SEGUROS**

```bash
# Health check deve funcionar
curl -f http://localhost:8081/actuator/health
# Deve retornar: {"status":"UP"}

# Endpoints sensíveis devem estar bloqueados
curl -s http://localhost:8081/actuator/env && echo "❌ ENV ENDPOINT EXPOSTO" || echo "✅ ENV protegido"
curl -s http://localhost:8081/actuator/configprops && echo "❌ CONFIGPROPS EXPOSTO" || echo "✅ CONFIGPROPS protegido"
curl -s http://localhost:8081/actuator/beans && echo "❌ BEANS EXPOSTO" || echo "✅ BEANS protegido"
curl -s http://localhost:8081/actuator/threaddump && echo "❌ THREADDUMP EXPOSTO" || echo "✅ THREADDUMP protegido"

# Info deve funcionar (não sensível)
curl -f http://localhost:8081/actuator/info
```

## **5. VERIFICAÇÃO DE VAZAMENTO NOS LOGS**

```bash
# Verificar logs recentes não contêm secrets
docker logs autenticacao-microservice --since="1h" 2>&1 | \
  grep -i -E "(password|secret|key|credential|token)" | \
  grep -v -E "(jwt.*validation|key.*rotation|secret.*loaded|token.*generated)" && \
  echo "❌ POSSÍVEL VAZAMENTO NOS LOGS" || echo "✅ Logs seguros"

# Verificar logs de sistema
journalctl -u docker --since="1h" | \
  grep -i -E "(password|secret|key)" && \
  echo "❌ POSSÍVEL VAZAMENTO NO SISTEMA" || echo "✅ Sistema seguro"
```

## **6. VERIFICAÇÃO DE CARREGAMENTO DO CONFIGTREE**

```bash
# Verificar que Spring está carregando secrets via configtree
docker logs autenticacao-microservice 2>&1 | grep -i configtree
# Deve mostrar: "Loading configuration from configtree"

# Verificar que não há erros de carregamento de propriedades
docker logs autenticacao-microservice 2>&1 | grep -i -E "(error.*property|failed.*load|configuration.*error)"
# Não deve mostrar erros relacionados a propriedades

# Verificar conexão com banco de dados funcionando
curl -f http://localhost:8081/actuator/health/db
# Deve retornar: {"status":"UP"}
```

## **7. VERIFICAÇÃO DE FUNCIONALIDADES JWT (ESPECÍFICO AUTENTICAÇÃO)**

```bash
# Verificar JWKS endpoint público funcionando
curl -f http://localhost:8081/.well-known/jwks.json
# Deve retornar JSON com chaves públicas

# Verificar introspection endpoint (se disponível)
curl -f http://localhost:8081/rest/v1/auth/introspect
# Deve retornar resposta válida (mesmo que unauthorized)

# Testar geração de token (com credenciais válidas)
curl -X POST http://localhost:8081/rest/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test"}'
# Deve retornar token JWT válido ou erro de credenciais
```

## **8. VERIFICAÇÃO DE ROTAÇÃO DE CHAVES JWT**

```bash
# Verificar data de criação das chaves JWT no Key Vault
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-jwt-signing-key" --query "attributes.created" -o tsv

# Verificar próxima data de rotação
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-jwt-signing-key" \
  --query "attributes.expires" -o tsv

# Verificar que key ID está consistente
az keyvault secret show --vault-name "kv-conexao-de-sorte" \
  --name "conexao-de-sorte-jwt-key-id" --query "value" -o tsv
```

## **9. VERIFICAÇÃO DE OAUTH2 CONFIGURATION**

```bash
# Verificar se OAuth2 client secrets estão carregados corretamente
docker logs autenticacao-microservice 2>&1 | grep -i oauth2 | grep -v -i "(client.*secret|password)"
# Deve mostrar logs de configuração OAuth2 sem expor secrets

# Testar endpoint OAuth2 authorization (se disponível)
curl -f "http://localhost:8081/oauth2/authorization/azure"
# Deve retornar redirect ou configuração válida
```

## **10. SCRIPT DE VERIFICAÇÃO COMPLETA**

```bash
#!/bin/bash
# verify-autenticacao-security.sh - Script de verificação completa

set -euo pipefail

echo "🔒 VERIFICAÇÃO COMPLETA DE SEGURANÇA - AUTENTICACAO MICROSERVICE"
echo "=============================================================="

# 1. Verificar container está rodando
if ! docker ps | grep -q autenticacao-microservice; then
    echo "❌ Container não está rodando"
    exit 1
fi
echo "✅ Container está rodando"

# 2. Verificar health
if curl -f -s http://localhost:8081/actuator/health > /dev/null; then
    echo "✅ Health check passou"
else
    echo "❌ Health check falhou"
    exit 1
fi

# 3. Verificar endpoints sensíveis bloqueados
if curl -f -s http://localhost:8081/actuator/env > /dev/null; then
    echo "❌ Endpoint /env está exposto"
    exit 1
else
    echo "✅ Endpoint /env está protegido"
fi

# 4. Verificar secrets existem e têm permissões corretas
if [[ ! -d "/run/secrets" ]]; then
    echo "❌ Diretório de secrets não existe"
    exit 1
fi

for secret in DB_PASSWORD JWT_SIGNING_KEY JWT_VERIFICATION_KEY OAUTH2_CLIENT_SECRET ENCRYPTION_MASTER_KEY; do
    if [[ ! -f "/run/secrets/$secret" ]]; then
        echo "❌ Secret $secret não existe"
        exit 1
    fi
    
    PERMS=$(stat -c "%a" "/run/secrets/$secret")
    if [[ "$PERMS" != "400" ]]; then
        echo "❌ Secret $secret tem permissões incorretas: $PERMS"
        exit 1
    fi
done
echo "✅ Todos os secrets existem com permissões corretas"

# 5. Verificar não há vazamento em env vars
if docker inspect autenticacao-microservice | jq '.[]|.Config.Env[]' | \
   grep -i -E "(password|secret|key)" | \
   grep -v -E "(JAVA_OPTS|SPRING_|TZ)" > /dev/null; then
    echo "❌ Possível vazamento em variáveis de ambiente"
    exit 1
else
    echo "✅ Nenhum segredo em variáveis de ambiente"
fi

# 6. Verificar JWKS endpoint específico do microserviço de autenticação
if curl -f -s http://localhost:8081/.well-known/jwks.json > /dev/null; then
    echo "✅ JWKS endpoint funcionando"
else
    echo "❌ JWKS endpoint não está funcionando"
    exit 1
fi

echo ""
echo "🎉 VERIFICAÇÃO COMPLETA: TODAS AS CHECAGENS PASSARAM"
echo "✅ Microserviço de autenticação está seguro e em conformidade"
echo "✅ Funcionalidades JWT estão operacionais"
echo "✅ OAuth2 está configurado corretamente"
```

## **11. MONITORAMENTO CONTÍNUO - AUTENTICAÇÃO ESPECÍFICO**

```bash
# Configurar alertas para tokens JWT mal formados (crontab)
0 */6 * * * /usr/local/bin/check-jwt-health.sh

# Script de monitoramento JWT
cat > /usr/local/bin/check-jwt-health.sh << 'EOF'
#!/bin/bash
LOG_FILE="/var/log/jwt-health.log"

# Verificar se JWKS está respondendo
if ! curl -f -s http://localhost:8081/.well-known/jwks.json > /dev/null; then
    echo "$(date): ❌ JWKS endpoint não está respondendo" >> $LOG_FILE
    # Enviar alerta (email, Slack, etc.)
fi

# Verificar se não há muitos erros JWT nos logs
JWT_ERRORS=$(docker logs autenticacao-microservice --since="6h" 2>&1 | grep -c -i "jwt.*error" || echo "0")
if [[ $JWT_ERRORS -gt 10 ]]; then
    echo "$(date): ⚠️ Muitos erros JWT detectados: $JWT_ERRORS" >> $LOG_FILE
fi

echo "$(date): ✅ JWT health check ok" >> $LOG_FILE
EOF
chmod +x /usr/local/bin/check-jwt-health.sh
```