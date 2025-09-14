# 🔐 Segredos Padronizados - Conexão de Sorte Gateway

## ⚠️ **Problema Resolvido: Redis WRONGPASS**

Este documento padroniza a nomenclatura de segredos para evitar duplicações e inconsistências que causavam erros como `WRONGPASS invalid username-password pair`.

## 📋 **Mapeamento de Segredos - Padrão ÚNICO (OIDC-only)**

### **🔴 Redis Configuration (obrigatórios no runtime)**
```yaml
# ✅ PADRÃO ÚNICO - USE APENAS ESTES
REDIS_HOST          ↔ conexao-de-sorte-redis-host          ↔ conexao-redis
REDIS_PORT          ↔ conexao-de-sorte-redis-port          ↔ 6379  
REDIS_PASSWORD      ↔ conexao-de-sorte-redis-password      ↔ [senha-do-redis]
REDIS_DATABASE      ↔ conexao-de-sorte-redis-database      ↔ 1
```

### **🔴 Database Configuration (obrigatórios no runtime)**
```yaml
# ✅ PADRÃO ÚNICO - USE APENAS ESTES  
DATABASE_JDBC_URL   ↔ conexao-de-sorte-database-jdbc-url   ↔ jdbc:mysql://...
DATABASE_R2DBC_URL  ↔ conexao-de-sorte-database-r2dbc-url  ↔ r2dbc:mysql://...
DATABASE_USERNAME   ↔ conexao-de-sorte-database-username   ↔ [usuario]
DATABASE_PASSWORD   ↔ conexao-de-sorte-database-password   ↔ [senha]
DATABASE_HOST       ↔ conexao-de-sorte-database-host       ↔ conexao-mysql
DATABASE_PORT       ↔ conexao-de-sorte-database-port       ↔ 3306
```

### **🔴 JWT Configuration**
```yaml
# ✅ PADRÃO ÚNICO - USE APENAS ESTES
JWT_SECRET          ↔ conexao-de-sorte-jwt-secret          ↔ [jwt-secret]
JWT_ISSUER          ↔ conexao-de-sorte-jwt-issuer          ↔ https://auth.conexaodesorte.com.br
JWT_JWKS_URI        ↔ conexao-de-sorte-jwt-jwks-uri        ↔ https://auth.conexaodesorte.com.br/.well-known/jwks.json
JWT_KEY_ID          ↔ conexao-de-sorte-jwt-key-id          ↔ gateway-key
JWT_SIGNING_KEY     ↔ conexao-de-sorte-jwt-signing-key     ↔ [chave-assinatura]
JWT_VERIFICATION_KEY ↔ conexao-de-sorte-jwt-verification-key ↔ [chave-verificacao]
JWT_PRIVATE_KEY     ↔ conexao-de-sorte-jwt-privateKey      ↔ [chave-privada]
JWT_PUBLIC_KEY      ↔ conexao-de-sorte-jwt-publicKey       ↔ [chave-publica]
```

### **🔴 CORS & SSL Configuration**
```yaml
# ✅ PADRÃO ÚNICO - USE APENAS ESTES
CORS_ALLOWED_ORIGINS ↔ conexao-de-sorte-cors-allowed-origins ↔ https://conexaodesorte.com.br,https://www.conexaodesorte.com.br
CORS_ALLOW_CREDENTIALS ↔ conexao-de-sorte-cors-allow-credentials ↔ true
SSL_ENABLED         ↔ conexao-de-sorte-ssl-enabled         ↔ false
SSL_KEYSTORE_PATH   ↔ conexao-de-sorte-ssl-keystore-path   ↔ [caminho-keystore]
SSL_KEYSTORE_PASSWORD ↔ conexao-de-sorte-ssl-keystore-password ↔ [senha-keystore]
```

### **🔴 Encryption Configuration**
```yaml
# ✅ PADRÃO ÚNICO - USE APENAS ESTES
ENCRYPTION_MASTER_KEY      ↔ conexao-de-sorte-encryption-master-key      ↔ [chave-mestra]
ENCRYPTION_MASTER_PASSWORD ↔ conexao-de-sorte-encryption-master-password ↔ [senha-mestra]
ENCRYPTION_BACKUP_KEY      ↔ conexao-de-sorte-encryption-backup-key      ↔ [chave-backup]
```

## ❌ **Segredos REMOVIDOS (Duplicados) e Fallbacks proibidos**

### **Redis - REMOVIDOS**
- ~~`conexao-de-sorte-redis-host`~~ → Use `REDIS_HOST`
- ~~`conexao-de-sorte-redis-port`~~ → Use `REDIS_PORT`  
- ~~`conexao-de-sorte-redis-password`~~ → Use `REDIS_PASSWORD`
- ~~`conexao-de-sorte-redis-database`~~ → Use `REDIS_DATABASE`

### **Database - REMOVIDOS**  
- ~~`conexao-de-sorte-db-host`~~ → Use `DATABASE_HOST`
- ~~`conexao-de-sorte-db-port`~~ → Use `DATABASE_PORT`
- ~~`conexao-de-sorte-db-username`~~ → Use `DATABASE_USERNAME`
- ~~`conexao-de-sorte-db-password`~~ → Use `DATABASE_PASSWORD`
- ~~`conexao-de-sorte-database-url`~~ → Use `DATABASE_JDBC_URL`

## 🔧 **Configuração no Spring Boot (OIDC-only)**

Sem configtree, sem defaults. A aplicação falha caso as variáveis obrigatórias não estejam presentes ou não possam ser resolvidas via Key Vault.

Principais variáveis exigidas:
- `DATABASE_R2DBC_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_DATABASE`
- Para Key Vault: `AZURE_KEYVAULT_ENABLED=true`, `AZURE_TENANT_ID`, `AZURE_CLIENT_ID`, `AZURE_KEYVAULT_ENDPOINT`

## 📝 **Scripts de Correção**

### **Corrigir Redis WRONGPASS**
```bash
./scripts/fix-redis-secrets.sh
```

### **Sincronizar Azure Key Vault via CI (OIDC)**
Os segredos devem ser provisionados no Key Vault e acessados pela aplicação com OIDC. Não executar Azure CLI no runtime.

## 🎯 **Benefícios da Padronização**

✅ **Sem duplicações** de segredos  
✅ **OIDC** via GitHub Actions + Azure Key Vault  
❌ **Sem fallback** entre fontes de segredos  
✅ **Redução de erros** WRONGPASS e similar  
✅ **Manutenção simplificada** do ambiente  

## 🚨 **Regras de Nomenclatura**

1. **Docker Secrets**: `SNAKE_CASE` maiúsculo (ex: `REDIS_PASSWORD`)
2. **Azure Key Vault**: `kebab-case` com prefixo (ex: `conexao-de-sorte-redis-password`)  
3. **Spring Properties**: Sem fallback (ex: `${REDIS_PASSWORD}`)

## ⚡ **Resolução do Problema Redis**

O erro `WRONGPASS invalid username-password pair` foi causado por:

1. **Inconsistência** entre nomes de segredos
2. **Mapeamento incorreto** Docker Secrets ↔ Azure Key Vault
3. **Configuração Spring** buscando segredo inexistente

**Solução aplicada:**
- ✅ Padronização de nomenclatura
- ✅ Configuração híbrida com fallback
- ✅ Script de sincronização automática
- ✅ Validação de conectividade Redis

---
**🤖 Gerado por Claude Code** | **📅 2024-09-14**
