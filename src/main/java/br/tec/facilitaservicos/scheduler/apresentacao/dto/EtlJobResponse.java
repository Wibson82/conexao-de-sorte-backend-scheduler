package br.tec.facilitaservicos.scheduler.apresentacao.dto;

/**
 * Response para job de ETL iniciado.
 */
public record EtlJobResponse(
    String jobId
) {}