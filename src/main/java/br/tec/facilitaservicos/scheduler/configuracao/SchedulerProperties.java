package br.tec.facilitaservicos.scheduler.configuracao;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriedades de configuração para o Scheduler ETL.
 */
@ConfigurationProperties(prefix = "scheduler")
public record SchedulerProperties(
    Etl etl
) {
    public record Etl(
        Loterias loterias
    ) {}
    
    public record Loterias(
        boolean enabled,
        long backoffMs,
        int maxRetries
    ) {}
}