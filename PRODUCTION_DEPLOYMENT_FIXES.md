# 🚨 CORREÇÕES CRÍTICAS DE PRODUÇÃO - MICROSERVIÇO AUTENTICAÇÃO

## ⚠️ PROBLEMAS IDENTIFICADOS NO LOG DE PRODUÇÃO

### 1. **PERFIL DEV EM PRODUÇÃO** - CRÍTICO ❌
**Problema**: Serviço executando com `spring.profiles.active=dev`
**Solução**: Definir `SPRING_PROFILES_ACTIVE=prod` no deployment

### 2. **JDWP DEBUG EXPOSTO** - VULNERABILIDADE CRÍTICA 🔴
**Problema**: JDWP porta 5005 exposta (`-agentlib:jdwp:transport=dt_socket,server=y,suspend=n,address=5005`)
**Solução**: Remover parâmetro JDWP do comando de execução

### 3. **HEALTH CHECKS BLOQUEADOS** - OPERACIONAL ⚠️
**Problema**: `/actuator/health/readiness` retornando "Access Denied"
**Solução**: ✅ Já corrigido no `SecurityConfig.java`

### 4. **KEY VAULT DESABILITADO** - SEGURANÇA 🔐
**Problema**: Usando chaves de fallback ao invés do Azure Key Vault
**Solução**: ✅ Já corrigido no `application.yml` (prod profile)

---

## 🛠️ INSTRUÇÕES DE CORREÇÃO PARA DEPLOYMENT

### **AMBIENTE: KUBERNETES/DOCKER**

#### 1. Corrigir Environment Variables no Deployment:
```yaml
# deployment.yaml ou docker-compose.yml
environment:
  - SPRING_PROFILES_ACTIVE=prod  # ← CRÍTICO: Mudar de "dev" para "prod"
  
  # Azure Key Vault via Spring Cloud Azure (obrigatório para produção)
  - spring.cloud.azure.keyvault.secret.enabled=true
  - AZURE_CLIENT_ID=${AZURE_CLIENT_ID}
  - AZURE_KEYVAULT_ENDPOINT=${AZURE_KEYVAULT_ENDPOINT}
  - AZURE_TENANT_ID=${AZURE_TENANT_ID}
  - AZURE_SUBSCRIPTION_ID=${AZURE_SUBSCRIPTION_ID}
  
  # SSL habilitado
  - conexao-de-sorte-ssl-enabled=true
  - conexao-de-sorte-ssl-keystore-path=/app/ssl/keystore.p12
  - conexao-de-sorte-ssl-keystore-password=${SSL_KEYSTORE_PASSWORD}
```

#### 2. Corrigir Comando de Execução Java:
```bash
# ❌ COMANDO ATUAL (INSEGURO):
java -agentlib:jdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar app.jar

# ✅ COMANDO CORRETO (SEGURO):
java -Xmx2g -XX:+UseG1GC -Dspring.profiles.active=prod -jar app.jar
```

#### 3. Dockerfile Produção:
```dockerfile
FROM openjdk:25-jdk-slim

# Não expor porta de debug
# EXPOSE 5005  ← REMOVER ESTA LINHA

EXPOSE 8081 8443

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx2g -XX:+UseG1GC"

CMD ["java", "${JAVA_OPTS}", "-jar", "/app/autenticacao.jar"]
```

### **AMBIENTE: VM/SERVIDOR TRADICIONAL**

#### 1. Script de Start Corrigido:
```bash
#!/bin/bash
# start-auth-service.sh

export SPRING_PROFILES_ACTIVE=prod
export JAVA_HOME=/opt/java/25

# Variáveis do Azure Key Vault via Spring Cloud Azure
export spring_cloud_azure_keyvault_secret_enabled=true
export AZURE_CLIENT_ID="valor-do-secret"
export AZURE_KEYVAULT_ENDPOINT="valor-do-secret"
export AZURE_TENANT_ID="valor-do-secret"

# SSL
export conexao-de-sorte-ssl-enabled=true

# ✅ Comando seguro (sem JDWP)
java -Xmx2g -XX:+UseG1GC \
     -Dspring.profiles.active=prod \
     -jar /opt/apps/autenticacao/autenticacao.jar
```

---

## 🔍 VALIDAÇÃO PÓS-CORREÇÃO

### 1. **Verificar Perfil Correto**:
```bash
curl http://localhost:8081/actuator/info
# Deve retornar: "profiles": ["prod"]
```

### 2. **Health Checks Funcionando**:
```bash
curl http://localhost:8081/actuator/health/readiness
# Deve retornar: {"status":"UP"}
```

### 3. **JDWP Não Exposto**:
```bash
netstat -tlnp | grep 5005
# Não deve retornar nenhuma linha
```

### 4. **Key Vault Ativo**:
```bash
curl http://localhost:8081/actuator/info
# Deve mostrar Key Vault enabled: true nos logs
```

---

## 🎯 RESUMO DAS MUDANÇAS REALIZADAS

### ✅ **Arquivos Corrigidos**:

1. **`application.yml`**:
   - ✅ Azure Key Vault habilitado por padrão
   - ✅ SSL habilitado no perfil prod
   - ✅ JWT Key Vault configurado
   - ✅ CORS baseado em variáveis de ambiente

2. **`SecurityConfig.java`**:
   - ✅ Health checks públicos (`/actuator/health/**`)
   - ✅ Readiness/Liveness probes liberados
   - ✅ Prometheus metrics públicos
   - ✅ CORS dinâmico baseado em env vars

3. **Perfil de Produção**:
   - ✅ Key Vault obrigatório
   - ✅ Logs em nível INFO
   - ✅ Erro details desabilitados
   - ✅ Rate limiting mais restritivo

---

## ⚡ **AÇÃO IMEDIATA NECESSÁRIA**

1. **Definir `SPRING_PROFILES_ACTIVE=prod`** no deployment
2. **Remover parâmetro `-agentlib:jdwp`** do comando Java
3. **Configurar secrets do Azure Key Vault** nas variáveis de ambiente
4. **Reiniciar o serviço** para aplicar as correções

**PRIORIDADE**: 🔴 CRÍTICA - Executar imediatamente