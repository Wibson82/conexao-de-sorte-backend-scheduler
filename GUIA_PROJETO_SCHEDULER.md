# ⏰ Guia do Projeto: Scheduler
## Microserviço de Agendamento e Tarefas Automáticas

> **🎯 Contexto**: Microserviço responsável pelo agendamento e execução de tarefas automáticas, jobs recorrentes, processamento em lote e orquestração de workflows na plataforma.

---

## 📋 INFORMAÇÕES DO PROJETO

### **Identificação:**
- **Nome**: conexao-de-sorte-backend-scheduler
- **Porta**: 8085
- **Rede Principal**: conexao-network-swarm
- **Database**: conexao_scheduler (MySQL 8.4)
- **Runner**: `[self-hosted, Linux, X64, conexao, conexao-de-sorte-backend-scheduler]`

### **Tecnologias Específicas:**
- Spring Boot 3.5.5 + Spring WebFlux (reativo)
- R2DBC MySQL (persistência reativa)
- Spring Scheduler + Quartz (advanced scheduling)
- Redis (distributed locking + job queues)
- Job orchestration patterns

---

## 🗄️ ESTRUTURA DO BANCO DE DADOS

### **Database**: `conexao_scheduler`

#### **Tabelas:**
1. **`jobs_agendados`** - Jobs/tarefas agendadas
2. **`execucoes_job`** - Histórico de execuções
3. **`workflows`** - Workflows complexos

#### **Estrutura das Tabelas:**
```sql
-- jobs_agendados
id (String PK)
nome_job (String)                -- Nome identificador do job
descricao (String)
tipo_job (String)                -- CRON, FIXED_RATE, FIXED_DELAY, ONE_TIME
expressao_cron (String)          -- Expressão cron (se aplicável)
intervalo_ms (Long)              -- Intervalo em ms (se aplicável)
servico_alvo (String)            -- Microserviço que executará
endpoint_execucao (String)       -- Endpoint para chamada
payload_json (JSON)             -- Dados para execução
ativo (Boolean)
max_tentativas (Integer)
timeout_segundos (Integer)
prioridade (String)             -- LOW, NORMAL, HIGH, CRITICAL
grupo_job (String)              -- Agrupamento lógico
dependencias (JSON)            -- Jobs dependentes
condicoes_execucao (JSON)      -- Condições para execução
criado_por (String)
criado_em (DateTime)
atualizado_em (DateTime)
proxima_execucao (DateTime)

-- execucoes_job
id (String PK)
job_id (String)                 -- FK para jobs_agendados
inicio_execucao (DateTime)
fim_execucao (DateTime)
status (String)                 -- PENDING, RUNNING, SUCCESS, FAILED, TIMEOUT
resultado (JSON)                -- Resultado da execução
erro_mensagem (String)
tentativa_numero (Integer)
duracao_ms (Long)
servidor_execucao (String)      -- Servidor que executou
recursos_utilizados (JSON)     -- CPU, memória, etc.
logs_execucao (TEXT)

-- workflows
id (String PK)
nome_workflow (String)
descricao (String)
definicao_json (JSON)          -- Definição completa do workflow
jobs_sequencia (JSON)         -- Ordem de execução dos jobs
condicoes_fluxo (JSON)         -- Condições para ramificação
status (String)                -- ACTIVE, INACTIVE, PAUSED
versao (Integer)
criado_por (String)
criado_em (DateTime)
atualizado_em (DateTime)
```

#### **Relacionamentos:**
- execucoes_job.job_id → jobs_agendados.id

### **Configuração R2DBC:**
```yaml
r2dbc:
  url: r2dbc:mysql://mysql-proxy:6033/conexao_scheduler
  pool:
    initial-size: 2
    max-size: 15
```

---

## 🔐 SECRETS ESPECÍFICOS

### **Azure Key Vault Secrets Utilizados:**
```yaml
# Database
conexao-de-sorte-database-r2dbc-url
conexao-de-sorte-database-username
conexao-de-sorte-database-password

# Redis (distributed locking)
conexao-de-sorte-redis-host
conexao-de-sorte-redis-password
conexao-de-sorte-redis-port

# JWT for service-to-service calls
conexao-de-sorte-jwt-secret
conexao-de-sorte-jwt-verification-key

# Job Execution Security
conexao-de-sorte-scheduler-api-key
conexao-de-sorte-job-encryption-key

# External Services
conexao-de-sorte-email-scheduler-key
conexao-de-sorte-backup-scheduler-key
```

### **Cache Redis Específico:**
```yaml
redis:
  database: 10
  cache-names:
    - scheduler:locks
    - scheduler:job-queue
    - scheduler:execution-results
    - scheduler:workflow-state
```

---

## 🌐 INTEGRAÇÃO DE REDE

### **Comunicação Entrada (Server):**
- **Admin Interface** → Scheduler (gerenciamento de jobs)
- **Gateway** → Scheduler (rotas /api/scheduler/*)
- **Outros microserviços** → Scheduler (agendar jobs)

### **Comunicação Saída (Client):**
- Scheduler → **Todos os microserviços** (execução de jobs)
- Scheduler → **Financeiro** (relatórios automáticos)
- Scheduler → **Resultados** (cálculos periódicos)
- Scheduler → **Notificações** (envios agendados)
- Scheduler → **Auditoria** (jobs de compliance)
- Scheduler → **Backup Services** (backups automáticos)

### **Portas e Endpoints:**
```yaml
server.port: 8085

# Job Management
POST   /scheduler/jobs           # Criar job
GET    /scheduler/jobs           # Listar jobs
GET    /scheduler/jobs/{id}      # Detalhes do job
PUT    /scheduler/jobs/{id}      # Atualizar job
DELETE /scheduler/jobs/{id}      # Remover job
POST   /scheduler/jobs/{id}/execute  # Executar manualmente

# Execution Management
GET    /scheduler/execucoes      # Histórico execuções
GET    /scheduler/execucoes/{id} # Detalhes execução
POST   /scheduler/execucoes/{id}/retry  # Reexecutar

# Workflow Management
POST   /scheduler/workflows      # Criar workflow
GET    /scheduler/workflows      # Listar workflows
GET    /scheduler/workflows/{id} # Detalhes workflow
POST   /scheduler/workflows/{id}/start  # Iniciar workflow

# Admin & Monitoring
GET    /scheduler/status         # Status do scheduler
GET    /scheduler/metrics        # Métricas de jobs
POST   /scheduler/pause          # Pausar scheduler
POST   /scheduler/resume         # Retomar scheduler

GET    /actuator/health
```

---

## 🔗 DEPENDÊNCIAS CRÍTICAS

### **Serviços Dependentes (Upstream):**
1. **MySQL** (mysql-proxy:6033) - Persistência de jobs
2. **Redis** (conexao-redis:6379) - Distributed locking
3. **Azure Key Vault** - Secrets management

### **Serviços Consumidores (Downstream):**
- **Financeiro** - Relatórios automáticos, fechamento períodos
- **Resultados** - Cálculos de ranking, estatísticas
- **Notificações** - Campanhas agendadas, lembretes
- **Auditoria** - Jobs de compliance, limpeza logs
- **Backup Services** - Backups automáticos

### **Ordem de Deploy:**
```
1. MySQL + Redis (infrastructure)
2. Scheduler (orchestration service)
3. Target microservices (job executors)
```

---

## 🚨 ESPECIFICIDADES DO SCHEDULER

### **Tipos de Jobs Suportados:**
```yaml
job-types:
  cron: "0 0 2 * * *"           # Baseado em expressão cron
  fixed-rate: 300000            # A cada 5 minutos
  fixed-delay: 60000            # 1 minuto após terminar
  one-time: "2024-12-31T23:59"  # Execução única
```

### **Jobs Pré-configurados:**
```yaml
default-jobs:
  backup-database:
    cron: "0 0 3 * * SUN"       # Domingo 03:00
    target: backup-service
    
  calculate-rankings:
    cron: "0 */15 * * * *"      # A cada 15 minutos
    target: resultados:8083
    
  send-daily-reports:
    cron: "0 0 8 * * MON-FRI"   # Dias úteis 08:00
    target: notificacoes:8087
    
  cleanup-audit-logs:
    cron: "0 0 2 * * *"         # Diário 02:00
    target: auditoria:8082
    
  process-financial-batch:
    cron: "0 30 23 * * *"       # Diário 23:30
    target: financeiro:8086
```

### **Distributed Locking:**
```yaml
locking:
  provider: redis
  lease-time: 300s              # 5 minutos max
  wait-time: 10s                # Tempo de espera
  retry-attempts: 3
```

---

## 📊 MÉTRICAS ESPECÍFICAS

### **Custom Metrics:**
- `scheduler_jobs_executed_total{job,status}` - Jobs executados
- `scheduler_execution_duration{job}` - Duração das execuções
- `scheduler_jobs_scheduled_total` - Jobs agendados
- `scheduler_jobs_failed_total{job,reason}` - Jobs falharam
- `scheduler_active_jobs_gauge` - Jobs ativos
- `scheduler_queue_size{priority}` - Tamanho das filas

### **Alertas Configurados:**
- Job failure rate > 10%
- Job execution time > 30min (exceto backups)
- Scheduler service down > 1min
- Redis lock failures > 0
- Job queue size > 1000
- Critical job failures > 0

---

## 🔧 CONFIGURAÇÕES ESPECÍFICAS

### **Application Properties:**
```yaml
# Scheduler Configuration
scheduler:
  enabled: true
  pool-size: 10                 # Thread pool para execução
  max-concurrent-jobs: 5
  default-timeout: 1800s        # 30 minutos
  max-retries: 3
  retry-delay: 60s
  
# Quartz Configuration (se necessário)
quartz:
  job-store-type: jdbc
  properties:
    org.quartz.scheduler.instanceId: AUTO
    org.quartz.scheduler.instanceName: ConexaoScheduler
    org.quartz.threadPool.threadCount: 10
    
# Distributed Locking
distributed-lock:
  provider: redis
  lease-time: 300s
  wait-time: 10s
  
# Job Execution
job-execution:
  timeout-default: 1800s
  max-payload-size: 1MB
  result-retention-days: 30
  
# Workflows
workflows:
  max-parallel-jobs: 3
  failure-strategy: STOP_ON_FIRST_FAILURE
  retry-failed-workflows: true
```

### **Job Categories:**
```yaml
job-categories:
  maintenance:
    priority: LOW
    max-execution-time: 3600s
    
  business:
    priority: NORMAL  
    max-execution-time: 1800s
    
  critical:
    priority: HIGH
    max-execution-time: 600s
    immediate-retry: true
    
  realtime:
    priority: CRITICAL
    max-execution-time: 60s
    immediate-retry: true
    max-retries: 1
```

---

## 🧪 TESTES E VALIDAÇÕES

### **Health Checks:**
```bash
# Health principal
curl -f http://localhost:8085/actuator/health

# Database connectivity
curl -f http://localhost:8085/actuator/health/db

# Redis connectivity  
curl -f http://localhost:8085/actuator/health/redis

# Scheduler status
curl -f http://localhost:8085/scheduler/status
```

### **Smoke Tests Pós-Deploy:**
```bash
# 1. Criar job teste
curl -X POST http://localhost:8085/scheduler/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT" \
  -d '{
    "nome_job": "test-job",
    "tipo_job": "ONE_TIME",
    "servico_alvo": "resultados",
    "endpoint_execucao": "/health",
    "proxima_execucao": "'$(date -u +%Y-%m-%dT%H:%M:%S)'"
  }'

# 2. Executar job manualmente
JOB_ID=$(curl -s http://localhost:8085/scheduler/jobs | jq -r '.[-1].id')
curl -X POST http://localhost:8085/scheduler/jobs/$JOB_ID/execute

# 3. Verificar execução
curl http://localhost:8085/scheduler/execucoes | jq '.[0]'

# 4. Testar distributed lock
curl -X POST http://localhost:8085/scheduler/admin/test-lock
```

---

## ⚠️ TROUBLESHOOTING

### **Problema: Jobs Não Executam**
```bash
# 1. Verificar status scheduler
curl http://localhost:8085/scheduler/status

# 2. Verificar jobs ativos
curl http://localhost:8085/scheduler/jobs | jq '.[] | select(.ativo == true)'

# 3. Verificar locks Redis
redis-cli -a $REDIS_PASS KEYS "scheduler:locks:*"

# 4. Logs de execução
docker service logs conexao-scheduler | grep "job execution"
```

### **Problema: Jobs Ficam Travados**
```bash
# 1. Verificar execuções em andamento
curl http://localhost:8085/scheduler/execucoes | jq '.[] | select(.status == "RUNNING")'

# 2. Limpar locks órfãos
redis-cli -a $REDIS_PASS DEL $(redis-cli -a $REDIS_PASS KEYS "scheduler:locks:*")

# 3. Reiniciar job específico
curl -X POST http://localhost:8085/scheduler/jobs/{job-id}/retry
```

### **Problema: Performance Baixa**
```bash
# 1. Thread pool status
curl http://localhost:8085/actuator/metrics/executor.active
curl http://localhost:8085/actuator/metrics/executor.queued

# 2. Job execution times
curl http://localhost:8085/actuator/metrics/scheduler.execution.duration

# 3. Database connections
curl http://localhost:8085/actuator/metrics/r2dbc.pool.connections
```

---

## 📋 CHECKLIST PRÉ-DEPLOY

### **Configuração:**
- [ ] Database `conexao_scheduler` criado
- [ ] Redis configurado para distributed locking
- [ ] JWT secrets configurados
- [ ] Thread pool dimensionado
- [ ] Jobs padrão configurados

### **Jobs Críticos:**
- [ ] Backup automático configurado
- [ ] Limpeza de logs agendada
- [ ] Relatórios automáticos configurados
- [ ] Processamento batch financeiro
- [ ] Cálculo de rankings automático

### **Integração:**
- [ ] Conectividade com todos microserviços alvo
- [ ] Timeouts configurados adequadamente
- [ ] Retry policies definidas
- [ ] Monitoring e alertas ativos

---

## 🔄 DISASTER RECOVERY

### **Backup Crítico:**
1. **Database scheduler** (jobs e histórico)
2. **Configurações de jobs críticos**
3. **Workflows em execução**
4. **Redis locks state** (pode ser perdido)

### **Recovery Procedure:**
1. Restore database scheduler
2. Clear Redis locks (force reset)
3. Restart scheduler service
4. Verify job configurations
5. Resume paused jobs
6. Validate critical job execution
7. Monitor for missed executions

### **Jobs Perdidos:**
- Jobs em execução no momento da falha
- Jobs agendados para período de downtime
- Strategy: Re-execution manual para jobs críticos

---

## 💡 OPERATIONAL NOTES

### **Capacity Planning:**
- Thread pool: 10 threads concurrent
- Job queue: máx 1000 jobs pendentes  
- Execution history: 30 dias retention
- Database growth: ~100MB/mês

### **Best Practices:**
- Jobs idempotentes (safe re-execution)
- Timeout adequados por categoria
- Distributed locking para jobs únicos
- Comprehensive logging and monitoring

### **Monitoramento 24/7:**
- Critical job execution success
- Job queue health
- Scheduler service availability
- Lock contention monitoring
- Performance metrics tracking

---

**📅 Última Atualização**: Setembro 2025  
**🏷️ Versão**: 1.0  
**⏰ Criticidade**: ALTA - Automação crítica da plataforma