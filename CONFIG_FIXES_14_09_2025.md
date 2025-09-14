# Correção de Problemas de Configuração do Scheduler

## Problemas Resolvidos

### 1. URL do R2DBC Vazia
**Problema**: `java.lang.IllegalArgumentException: URL does not start with the r2dbc scheme`

**Causa**: As variáveis de ambiente não estavam sendo definidas corretamente, resultando em uma URL vazia para o R2DBC.

**Solução**:
- Criado perfil `dev` para desenvolvimento local
- Criado perfil `container` para execução em Docker
- Definidos valores padrão apropriados para cada ambiente
- Corrigido o perfil padrão no `application.yml`

### 2. Perfil Incorreto em Execução
**Problema**: Aplicação executando com perfil `dev` mesmo em produção

**Solução**:
- Atualizado script de entrada para respeitar `SPRING_PROFILES_ACTIVE`
- Definido perfil padrão `container` no Dockerfile
- Ajustado docker-compose para usar perfil `container`

### 3. Configuração do Banco de Dados
**Problema**: URLs de conexão inadequadas para diferentes ambientes

**Solução**:
- **Dev**: `localhost:3311` para desenvolvimento local
- **Container**: `scheduler-mysql:3306` para docker-compose
- **Azure**: Configuração via Key Vault

## Arquivos Criados/Modificados

### Novos Arquivos
1. `src/main/resources/application-dev.yml` - Configuração para desenvolvimento
2. `src/main/resources/application-container.yml` - Configuração para containers

### Arquivos Modificados
1. `src/main/resources/application.yml` - Valores padrão corrigidos
2. `docker/healthcheck-entrypoint.sh` - Suporte a perfis dinâmicos
3. `docker-compose.yml` - Perfil alterado para `container`
4. `Dockerfile` - Perfil padrão definido

## Perfis de Configuração

### `dev` (Desenvolvimento Local)
- Banco: `localhost:3311`
- Redis: `localhost:6379`
- Azure Key Vault: Desabilitado
- Logs: DEBUG para pacotes do projeto

### `container` (Docker Compose)
- Banco: `scheduler-mysql:3306`
- Redis: `redis:6379`
- Azure Key Vault: Desabilitado
- Logs: INFO com DEBUG seletivo

### `azure` (Produção)
- Banco: Via Azure Key Vault
- Redis: Via Azure Key Vault
- Azure Key Vault: Habilitado
- Logs: INFO

## Como Usar

### Desenvolvimento Local
```bash
# Com MySQL local na porta 3311
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Docker Compose
```bash
docker-compose up scheduler-service
```

### Produção (Azure)
```bash
# Definir variáveis de ambiente para Azure
export SPRING_PROFILES_ACTIVE=azure
export AZURE_KEYVAULT_ENDPOINT=...
# etc.
```

## Verificação

Para verificar se as correções funcionaram:

1. **Logs de Inicialização**: Deve mostrar o perfil correto
   ```
   The following 1 profile is active: "container"
   ```

2. **Conexão R2DBC**: Não deve mais aparecer erro de URL vazia
   ```
   Started R2DBC connection pool
   ```

3. **Aplicação Iniciada**: Deve aparecer a mensagem de sucesso
   ```
   Started SchedulerApplication in X.XXX seconds
   ```