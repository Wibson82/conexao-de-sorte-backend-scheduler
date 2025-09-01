# Lições Aprendidas: Resolução de Problemas de Teste com R2DBC Auditing

## Resumo do Problema

Durante o desenvolvimento de testes para microserviços Spring Boot com R2DBC, enfrentamos problemas relacionados ao `r2dbcAuditingHandler` que causavam falhas nos testes de integração. Este documento consolida as lições aprendidas e estratégias para resolver problemas similares.

## Problemas Identificados

### 1. Erro Principal: `r2dbcAuditingHandler` Required Bean
```
Parameter 0 of method r2dbcAuditingHandler in org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration required a bean of type 'org.springframework.data.r2dbc.mapping.R2dbcMappingContext' that could not be found.
```

### 2. Problemas Secundários
- Conflitos de configuração entre `@WebFluxTest` e R2DBC
- Definições duplicadas de mocks
- Erros 403/500 em testes de autenticação
- Depreciação de `@MockBean` no Spring Boot 3.4.0

## Estratégias de Resolução

### 1. Análise Sistemática

**Passo 1: Identificar o Escopo do Problema**
- Executar todos os testes para mapear falhas
- Analisar logs de erro detalhadamente
- Identificar padrões entre testes que falham

**Passo 2: Isolar Componentes Problemáticos**
- Criar testes simples para validar endpoints básicos
- Separar problemas de configuração de problemas de lógica de negócio

### 2. Configuração de Teste Personalizada

**Solução Principal: WebFluxTestConfiguration**

Criar uma classe de configuração específica para testes:

```java
@TestConfiguration
@EnableConfigurationProperties
public class WebFluxTestConfiguration {
    
    // Excluir auto-configurações problemáticas
    @Bean
    @Primary
    public AuthService authService() {
        return Mockito.mock(AuthService.class);
    }
    
    @Bean
    @Primary
    public RefreshTokenRepository refreshTokenRepository() {
        return Mockito.mock(RefreshTokenRepository.class);
    }
    
    @Bean
    @Primary
    public R2dbcEntityTemplate r2dbcEntityTemplate() {
        return Mockito.mock(R2dbcEntityTemplate.class);
    }
    
    @Bean
    @Primary
    public R2dbcMappingContext r2dbcMappingContext() {
        return Mockito.mock(R2dbcMappingContext.class);
    }
    
    @Bean
    @Primary
    public WebClient webClient() {
        return Mockito.mock(WebClient.class);
    }
}
```

### 3. Configuração de Testes de Controller

**Anotações Essenciais:**

```java
@WebFluxTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = {
        ReactiveSecurityAutoConfiguration.class,
        ReactiveUserDetailsServiceAutoConfiguration.class,
        R2dbcDataAutoConfiguration.class,
        R2dbcRepositoriesAutoConfiguration.class
    }
)
@Import(WebFluxTestConfiguration.class)
class AuthControllerTest {
    // Usar @MockitoBean em vez de @MockBean (Spring Boot 3.4.0+)
    @MockitoBean
    private AuthService authService;
}
```

### 4. Migração de Anotações Depreciadas

**Spring Boot 3.4.0+ - Substituir @MockBean:**

```java
// Antes (depreciado)
@MockBean
private AuthService authService;

// Depois (recomendado)
@MockitoBean
private AuthService authService;
```

**Import necessário:**
```java
import org.springframework.test.context.bean.override.mockito.MockitoBean;
```

## Checklist de Resolução

### ✅ Passo a Passo para Novos Microserviços

1. **Identificar Dependências R2DBC**
   - [ ] Verificar se o microserviço usa R2DBC
   - [ ] Identificar beans relacionados a auditing
   - [ ] Mapear dependências de repositórios

2. **Criar Configuração de Teste**
   - [ ] Criar `WebFluxTestConfiguration.java`
   - [ ] Adicionar mocks para beans R2DBC essenciais
   - [ ] Excluir auto-configurações problemáticas

3. **Configurar Testes de Controller**
   - [ ] Usar `@WebFluxTest` com exclusões apropriadas
   - [ ] Importar configuração personalizada
   - [ ] Usar `@MockitoBean` em vez de `@MockBean`

4. **Validar e Iterar**
   - [ ] Executar testes individuais primeiro
   - [ ] Executar suite completa de testes
   - [ ] Resolver problemas específicos de lógica de negócio

## Armadilhas Comuns

### ❌ O que NÃO fazer:

1. **Não misturar configurações**
   - Evitar `@MockBean` e `@Bean` para o mesmo tipo
   - Não usar múltiplas configurações conflitantes

2. **Não ignorar exclusões**
   - Sempre excluir auto-configurações R2DBC em testes WebFlux
   - Não esquecer de excluir configurações de segurança se necessário

3. **Não usar anotações depreciadas**
   - Migrar de `@MockBean` para `@MockitoBean`
   - Verificar documentação de versão do Spring Boot

### ✅ Melhores Práticas:

1. **Configuração Centralizada**
   - Uma classe de configuração por tipo de teste
   - Reutilizar configurações entre testes similares

2. **Testes Incrementais**
   - Começar com testes simples (endpoint exists)
   - Adicionar complexidade gradualmente

3. **Documentação de Mudanças**
   - Documentar exclusões e suas razões
   - Manter histórico de problemas resolvidos

## Comandos Úteis para Diagnóstico

```bash
# Executar teste específico
mvn test -Dtest=AuthControllerTest#testSpecificMethod

# Executar com logs detalhados
mvn test -X

# Verificar relatórios de teste
cat target/surefire-reports/TEST-*.xml

# Executar apenas testes de uma classe
mvn test -Dtest=AuthControllerTest
```

## Versões e Compatibilidade

- **Spring Boot 3.4.0+**: Usar `@MockitoBean`
- **Spring Boot < 3.4.0**: Usar `@MockBean`
- **R2DBC**: Sempre excluir auto-configurações em testes WebFlux
- **WebFlux**: Usar `@WebFluxTest` com exclusões apropriadas

## Resultados Esperados

Após aplicar essas estratégias:
- ✅ Testes de configuração passam sem erros R2DBC
- ✅ Mocks funcionam corretamente
- ✅ Sem warnings de depreciação
- ✅ Tempo de execução otimizado
- ⚠️ Possíveis falhas restantes relacionadas à lógica de negócio (esperado)

## Conclusão

A resolução de problemas R2DBC em testes requer uma abordagem sistemática focada em:
1. Exclusão de auto-configurações conflitantes
2. Configuração centralizada de mocks
3. Uso de anotações atualizadas
4. Validação incremental

Este processo, quando seguido metodicamente, resolve a maioria dos problemas relacionados ao `r2dbcAuditingHandler` e permite focar na lógica de negócio dos testes.