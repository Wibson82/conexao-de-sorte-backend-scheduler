# 📚 LIÇÕES APRENDIDAS - MICROSERVIÇO AUTENTICAÇÃO

> **INSTRUÇÕES PARA AGENTES DE IA:** Este arquivo contém lições aprendidas críticas deste microserviço. SEMPRE atualize este arquivo após resolver problemas, implementar correções ou descobrir melhores práticas. Use o formato padronizado abaixo.

---

## 🎯 **METADADOS DO MICROSERVIÇO**
- **Nome:** conexao-de-sorte-backend-autenticacao
- **Responsabilidade:** Autenticação JWT, gerenciamento de usuários, tokens
- **Tecnologias:** Spring Boot 3.5.5, WebFlux, R2DBC, Java 25
- **Porta:** 8081
- **Última Atualização:** 2025-08-27

---

## ✅ **CORREÇÕES APLICADAS (2025-08-27)**

### 🔧 **1. Configuração Redis Padronizada**
**Problema:** Configuração Redis inconsistente entre microserviços
**Solução:** 
```yaml
spring:
  data:
    redis:
      database: 0  # DB 0 para autenticacao
      max-active: 20 (não max-total)
      max-wait: 3000ms
```
**Lição:** Sempre usar `spring.data.redis` e separar databases por microserviço (0=auth, 1=chat, 2=resultados, 4=notifications)

### 🔍 **2. Verificação de Segurança**
**Verificado:** ✅ Sem senhas hardcoded, ✅ SSL configurado, ✅ Circuit breakers presentes
**Lição:** Sempre verificar configurações de produção antes do deployment

---

## 🚨 **PROBLEMAS CONHECIDOS & SOLUÇÕES**

### ❌ **Spring Cloud Azure Compatibility**
**Sintoma:** `Spring Boot [3.5.5] is not compatible with this Spring Cloud release train`
**Solução:** Adicionar `spring.cloud.compatibility-verifier.enabled: false`
**Aplicável:** Apenas se usar Spring Cloud Azure (este microserviço NÃO usa)

### ❌ **Pool Redis Inadequado**
**Sintoma:** Conexões Redis esgotadas em produção
**Solução:** 
- `max-active: 20` (não 8)
- `min-idle: 2` (não 0)
- `max-wait: 3000ms`

---

## 🎯 **BOAS PRÁTICAS IDENTIFICADAS**

### ✅ **Configuração Recomendada Redis (Autenticação):**
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      database: 0  # Dedicado para autenticação
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 8
          min-idle: 2
          max-wait: 3000ms
```

### ✅ **Estrutura de Banco Recomendada:**
- Nome: `conexao_sorte_auth` (específico do microserviço)
- R2DBC para operações reativas
- Flyway para migrations com JDBC

---

## 🔄 **HISTÓRICO DE MUDANÇAS**

### **2025-08-27**
- ✅ .dockerignore versionado para reduzir contexto de build e evitar vazamento de segredos
- ✅ Defaults de rede Docker: preferir host interno (ex.: `conexao-mysql`) e evitar `localhost` dentro do container
- ✅ Padronizar segredos via Key Vault + configtree (`/run/secrets`) e aceitar `SPRING_DATASOURCE_*`/`DB_*`
- ✅ Separar R2DBC (r2dbc://) de Flyway (jdbc://) quando Flyway estiver ativo
- ✅ Desabilitar Redis quando não provisionado (auto-config e health)

### **2025-08-27**
- ✅ Redis: Corrigido `max-total` → `max-active`, adicionado `database: 0`
- ✅ Verificação segurança: Sem problemas encontrados
- ✅ Versões: Spring Boot 3.5.5, Java 25 mantidos

---

## 📋 **CHECKLIST PARA FUTURAS ALTERAÇÕES**

**Antes de qualquer mudança, verificar:**
- [ ] Configurações Redis seguem padrão (database separado, max-active)
- [ ] Sem senhas/secrets hardcoded
- [ ] Circuit breakers funcionais
- [ ] Timeouts adequados (2000ms Redis, 30s database)
- [ ] Logs não expõem informações sensíveis
- [ ] SSL habilitado para produção

**Após mudanças:**
- [ ] Testar conexão com Redis database 0
- [ ] Verificar performance do pool Redis
- [ ] Validar circuit breakers
- [ ] Atualizar este arquivo LESSONS_LEARNED.md

---

## 🤖 **INSTRUÇÕES PARA AGENTES DE IA**

1. **SEMPRE leia este arquivo** antes de fazer alterações neste microserviço
2. **SEMPRE atualize este arquivo** após resolver problemas
3. **Siga os padrões** definidos nas seções "Boas Práticas"
4. **Documente novos problemas** na seção "Problemas Conhecidos"
5. **Use o checklist** antes e depois de mudanças

---

*📝 Arquivo gerado automaticamente em 2025-08-27 por Claude Code*
*🔄 Próxima revisão recomendada: A cada correção significativa*
