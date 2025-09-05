package br.tec.facilitaservicos.scheduler.dominio.enums;

/**
 * ============================================================================
 * ⏰ TIPOS DE JOBS PARA AGENDAMENTO
 * ============================================================================
 * 
 * Enum que define os diferentes tipos de jobs no sistema de scheduler:
 * - ETL jobs para extração e transformação de dados
 * - Batch jobs para processamento em lote
 * - Webhook jobs para integrações em tempo real
 * - Monitoring jobs para health checks
 * - Maintenance jobs para limpeza e otimização
 * - Custom jobs definidos pelos usuários
 * 
 * Cada tipo tem configurações específicas:
 * - Timeout padrão
 * - Retry policy
 * - Prioridade base
 * - Resource requirements
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum TipoJob {

    // === EXTRAÇÃO DE DADOS (ETL) ===
    
    /**
     * ETL para extração de resultados de loterias
     * - Web scraping de sites oficiais
     * - Validação e limpeza de dados
     * - Transformação para formato padrão
     */
    ETL_RESULTADOS("etl_resultados", "ETL Resultados Loterias", 600, 5, true, false),
    
    /**
     * ETL genérico para outras fontes de dados
     */
    ETL_GENERICO("etl_generico", "ETL Genérico", 300, 3, true, false),
    
    /**
     * ETL para sincronização de dados entre sistemas
     */
    ETL_SINCRONIZACAO("etl_sync", "ETL Sincronização", 180, 3, true, false),

    // === PROCESSAMENTO BATCH ===
    
    /**
     * Processamento de dados em lote
     * - Cálculos estatísticos
     * - Agregações complexas
     * - Relatórios batch
     */
    BATCH_PROCESSAMENTO("batch_process", "Batch Processing", 1800, 3, true, true),
    
    /**
     * Backup e arquivamento de dados
     */
    BATCH_BACKUP("batch_backup", "Backup de Dados", 3600, 2, false, false),
    
    /**
     * Limpeza de dados antigos
     */
    BATCH_CLEANUP("batch_cleanup", "Limpeza de Dados", 900, 2, false, false),

    // === WEBHOOKS E INTEGRAÇÕES ===
    
    /**
     * Execução de webhooks para integrações
     */
    WEBHOOK("webhook", "Webhook Integration", 60, 5, true, true),
    
    /**
     * Notificações via webhook
     */
    WEBHOOK_NOTIFICATION("webhook_notification", "Webhook Notification", 30, 3, true, true),
    
    /**
     * Callbacks para sistemas externos
     */
    WEBHOOK_CALLBACK("webhook_callback", "Webhook Callback", 45, 3, true, true),

    // === MONITORAMENTO ===
    
    /**
     * Health checks de sistemas
     */
    MONITORING_HEALTH("monitoring_health", "Health Check", 30, 2, false, true),
    
    /**
     * Coleta de métricas
     */
    MONITORING_METRICS("monitoring_metrics", "Metrics Collection", 60, 2, false, true),
    
    /**
     * Verificação de SLA
     */
    MONITORING_SLA("monitoring_sla", "SLA Monitoring", 120, 1, false, false),

    // === MANUTENÇÃO ===
    
    /**
     * Otimização de banco de dados
     */
    MAINTENANCE_DB("maintenance_db", "Database Maintenance", 1200, 1, false, false),
    
    /**
     * Limpeza de cache
     */
    MAINTENANCE_CACHE("maintenance_cache", "Cache Cleanup", 300, 1, false, false),
    
    /**
     * Rotação de logs
     */
    MAINTENANCE_LOGS("maintenance_logs", "Log Rotation", 600, 1, false, false),

    // === NOTIFICAÇÕES ===
    
    /**
     * Envio de notificações agendadas
     */
    NOTIFICATION_SCHEDULED("notification_scheduled", "Scheduled Notification", 60, 3, true, true),
    
    /**
     * Digest de notificações
     */
    NOTIFICATION_DIGEST("notification_digest", "Notification Digest", 180, 2, true, false),
    
    /**
     * Lembretes automáticos
     */
    NOTIFICATION_REMINDER("notification_reminder", "Reminder", 30, 2, true, true),

    // === RELATÓRIOS ===
    
    /**
     * Geração de relatórios executivos
     */
    REPORT_EXECUTIVE("report_executive", "Executive Report", 900, 2, false, false),
    
    /**
     * Relatórios operacionais
     */
    REPORT_OPERATIONAL("report_operational", "Operational Report", 300, 2, false, false),
    
    /**
     * Relatórios de compliance
     */
    REPORT_COMPLIANCE("report_compliance", "Compliance Report", 600, 1, false, false),

    // === CUSTOM ===
    
    /**
     * Jobs customizados definidos pelo usuário
     */
    CUSTOM("custom", "Custom Job", 300, 3, true, false);

    private final String codigo;
    private final String descricao;
    private final int timeoutPadraoSegundos;
    private final int maxTentativasPadrao;
    private final boolean permiteRetry;
    private final boolean permiteConcorrencia;

    TipoJob(String codigo, String descricao, int timeoutPadraoSegundos, 
            int maxTentativasPadrao, boolean permiteRetry, boolean permiteConcorrencia) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.timeoutPadraoSegundos = timeoutPadraoSegundos;
        this.maxTentativasPadrao = maxTentativasPadrao;
        this.permiteRetry = permiteRetry;
        this.permiteConcorrencia = permiteConcorrencia;
    }

    // === MÉTODOS DE CLASSIFICAÇÃO ===

    /**
     * Verifica se é um job de ETL
     */
    public boolean isETL() {
        return this == ETL_RESULTADOS || this == ETL_GENERICO || this == ETL_SINCRONIZACAO;
    }

    /**
     * Verifica se é um job de batch processing
     */
    public boolean isBatch() {
        return this == BATCH_PROCESSAMENTO || this == BATCH_BACKUP || this == BATCH_CLEANUP;
    }

    /**
     * Verifica se é um job de webhook
     */
    public boolean isWebhook() {
        return this == WEBHOOK || this == WEBHOOK_NOTIFICATION || this == WEBHOOK_CALLBACK;
    }

    /**
     * Verifica se é um job de monitoramento
     */
    public boolean isMonitoramento() {
        return this == MONITORING_HEALTH || this == MONITORING_METRICS || this == MONITORING_SLA;
    }

    /**
     * Verifica se é um job de manutenção
     */
    public boolean isManutencao() {
        return this == MAINTENANCE_DB || this == MAINTENANCE_CACHE || this == MAINTENANCE_LOGS;
    }

    /**
     * Verifica se é um job de notificação
     */
    public boolean isNotificacao() {
        return this == NOTIFICATION_SCHEDULED || this == NOTIFICATION_DIGEST || 
               this == NOTIFICATION_REMINDER;
    }

    /**
     * Verifica se é um job de relatório
     */
    public boolean isRelatorio() {
        return this == REPORT_EXECUTIVE || this == REPORT_OPERATIONAL || this == REPORT_COMPLIANCE;
    }

    /**
     * Verifica se é um job crítico para o negócio
     */
    public boolean isCritico() {
        return this == ETL_RESULTADOS || this == MONITORING_HEALTH || 
               this == NOTIFICATION_SCHEDULED || this == BATCH_BACKUP;
    }

    /**
     * Verifica se deve ser executado em horário comercial
     */
    public boolean deveExecutarEmHorarioComercial() {
        return this == REPORT_EXECUTIVE || this == REPORT_OPERATIONAL || 
               this == ETL_RESULTADOS;
    }

    /**
     * Verifica se pode ser executado em paralelo
     */
    public boolean podeExecutarEmParalelo() {
        return permiteConcorrencia;
    }

    // === CONFIGURAÇÕES ESPECÍFICAS ===

    /**
     * Obtém o CRON expression padrão baseado no tipo
     */
    public String getCronPadrao() {
        return switch (this) {
            case ETL_RESULTADOS -> "0 0 */2 * * ?"; // A cada 2 horas
            case MONITORING_HEALTH -> "0 */5 * * * ?"; // A cada 5 minutos
            case MONITORING_METRICS -> "0 */1 * * * ?"; // A cada minuto
            case BATCH_BACKUP -> "0 0 2 * * ?"; // 2h da manhã
            case BATCH_CLEANUP -> "0 30 1 * * ?"; // 1h30 da manhã
            case MAINTENANCE_DB -> "0 0 3 * * SUN"; // Domingo 3h
            case MAINTENANCE_CACHE -> "0 0 */6 * * ?"; // A cada 6 horas
            case MAINTENANCE_LOGS -> "0 0 4 * * ?"; // 4h da manhã
            case REPORT_EXECUTIVE -> "0 0 8 * * MON"; // Segunda 8h
            case REPORT_OPERATIONAL -> "0 0 9 * * *"; // Todo dia 9h
            case NOTIFICATION_DIGEST -> "0 0 18 * * *"; // Todo dia 18h
            default -> "0 0 */1 * * ?"; // A cada hora (padrão)
        };
    }

    /**
     * Obtém o grupo padrão para o tipo de job
     */
    public String getGrupoPadrao() {
        return switch (this) {
            case ETL_RESULTADOS, ETL_GENERICO, ETL_SINCRONIZACAO -> "ETL";
            case BATCH_PROCESSAMENTO, BATCH_BACKUP, BATCH_CLEANUP -> "BATCH";
            case WEBHOOK, WEBHOOK_NOTIFICATION, WEBHOOK_CALLBACK -> "WEBHOOKS";
            case MONITORING_HEALTH, MONITORING_METRICS, MONITORING_SLA -> "MONITORING";
            case MAINTENANCE_DB, MAINTENANCE_CACHE, MAINTENANCE_LOGS -> "MAINTENANCE";
            case NOTIFICATION_SCHEDULED, NOTIFICATION_DIGEST, NOTIFICATION_REMINDER -> "NOTIFICATIONS";
            case REPORT_EXECUTIVE, REPORT_OPERATIONAL, REPORT_COMPLIANCE -> "REPORTS";
            case CUSTOM -> "CUSTOM";
        };
    }

    /**
     * Obtém a prioridade padrão baseada no tipo
     */
    public PrioridadeJob getPrioridadePadrao() {
        return switch (this) {
            case ETL_RESULTADOS, MONITORING_HEALTH -> PrioridadeJob.ALTA;
            case BATCH_BACKUP, NOTIFICATION_SCHEDULED -> PrioridadeJob.ALTA;
            case WEBHOOK, MONITORING_METRICS -> PrioridadeJob.NORMAL;
            case BATCH_PROCESSAMENTO, ETL_GENERICO -> PrioridadeJob.NORMAL;
            case MAINTENANCE_DB, MAINTENANCE_CACHE -> PrioridadeJob.BAIXA;
            case REPORT_EXECUTIVE, REPORT_COMPLIANCE -> PrioridadeJob.BAIXA;
            default -> PrioridadeJob.NORMAL;
        };
    }

    /**
     * Obtém o delay padrão para retry (em segundos)
     */
    public int getRetryDelayPadrao() {
        return switch (this) {
            case WEBHOOK, WEBHOOK_NOTIFICATION, WEBHOOK_CALLBACK -> 30; // 30 segundos
            case MONITORING_HEALTH, MONITORING_METRICS -> 60; // 1 minuto
            case ETL_RESULTADOS, BATCH_PROCESSAMENTO -> 300; // 5 minutos
            case NOTIFICATION_SCHEDULED -> 120; // 2 minutos
            default -> 60; // 1 minuto (padrão)
        };
    }

    /**
     * Verifica se requer recursos especiais (CPU/Memory)
     */
    public boolean requerRecursosEspeciais() {
        return this == BATCH_PROCESSAMENTO || this == ETL_RESULTADOS || 
               this == REPORT_EXECUTIVE || this == MAINTENANCE_DB;
    }

    // === MÉTODOS UTILITÁRIOS ===

    /**
     * Busca tipo de job por código
     */
    public static TipoJob porCodigo(String codigo) {
        for (TipoJob tipo : values()) {
            if (tipo.codigo.equalsIgnoreCase(codigo)) {
                return tipo;
            }
        }
        return CUSTOM; // Fallback
    }

    /**
     * Obtém todos os tipos de ETL
     */
    public static TipoJob[] getTiposETL() {
        return new TipoJob[]{ETL_RESULTADOS, ETL_GENERICO, ETL_SINCRONIZACAO};
    }

    /**
     * Obtém todos os tipos de batch
     */
    public static TipoJob[] getTiposBatch() {
        return new TipoJob[]{BATCH_PROCESSAMENTO, BATCH_BACKUP, BATCH_CLEANUP};
    }

    /**
     * Obtém todos os tipos críticos
     */
    public static TipoJob[] getTiposCriticos() {
        return java.util.Arrays.stream(values())
            .filter(TipoJob::isCritico)
            .toArray(TipoJob[]::new);
    }

    // === GETTERS ===

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getTimeoutPadraoSegundos() {
        return timeoutPadraoSegundos;
    }

    public int getMaxTentativasPadrao() {
        return maxTentativasPadrao;
    }

    public boolean isPermiteRetry() {
        return permiteRetry;
    }

    public boolean isPermiteConcorrencia() {
        return permiteConcorrencia;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", descricao, codigo);
    }
}