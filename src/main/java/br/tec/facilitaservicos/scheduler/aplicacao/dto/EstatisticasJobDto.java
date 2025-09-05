package br.tec.facilitaservicos.scheduler.aplicacao.dto;


/**
 * ============================================================================
 * 📊 DTO PARA ESTATÍSTICAS DE JOBS
 * ============================================================================
 * 
 * DTO para estatísticas e métricas dos jobs.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public class EstatisticasJobDto {

    private Long total;
    private Long concluidos;
    private Long falhados;
    private Long executando;
    private Double duracaoMediaMs;

    // Construtores
    public EstatisticasJobDto() {}

    public EstatisticasJobDto(Long total, Long concluidos, Long falhados, Long executando, Double duracaoMediaMs) {
        this.total = total;
        this.concluidos = concluidos;
        this.falhados = falhados;
        this.executando = executando;
        this.duracaoMediaMs = duracaoMediaMs;
    }

    // Getters e Setters
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }

    public Long getConcluidos() { return concluidos; }
    public void setConcluidos(Long concluidos) { this.concluidos = concluidos; }

    public Long getFalhados() { return falhados; }
    public void setFalhados(Long falhados) { this.falhados = falhados; }

    public Long getExecutando() { return executando; }
    public void setExecutando(Long executando) { this.executando = executando; }

    public Double getDuracaoMediaMs() { return duracaoMediaMs; }
    public void setDuracaoMediaMs(Double duracaoMediaMs) { this.duracaoMediaMs = duracaoMediaMs; }
    
    // Campos calculados
    public Double getTaxaSucesso() {
        if (total == null || total == 0) return 0.0;
        if (concluidos == null) return 0.0;
        return (concluidos.doubleValue() / total.doubleValue()) * 100.0;
    }
    
    public Double getTaxaFalha() {
        if (total == null || total == 0) return 0.0;
        if (falhados == null) return 0.0;
        return (falhados.doubleValue() / total.doubleValue()) * 100.0;
    }
    
    public String getDuracaoMediaFormatada() {
        if (duracaoMediaMs == null || duracaoMediaMs == 0) return "N/A";
        
        long millis = duracaoMediaMs.longValue();
        long segundos = millis / 1000;
        long minutos = segundos / 60;
        long horas = minutos / 60;
        
        if (horas > 0) {
            return String.format("%dh %dm %ds", horas, minutos % 60, segundos % 60);
        } else if (minutos > 0) {
            return String.format("%dm %ds", minutos, segundos % 60);
        } else {
            return String.format("%ds", segundos);
        }
    }
}