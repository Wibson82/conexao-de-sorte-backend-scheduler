package br.tec.facilitaservicos.scheduler.apresentacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request para iniciar um job de ETL de loterias.
 */
public record EtlJobRequest(
    @NotBlank(message = "Modalidade é obrigatória")
    String modalidade,
    
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Data deve estar no formato yyyy-MM-dd")
    String data
) {}