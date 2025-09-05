package br.tec.facilitaservicos.scheduler.aplicacao.dto;

import br.tec.facilitaservicos.scheduler.dominio.enums.StatusJob;
import br.tec.facilitaservicos.scheduler.dominio.enums.TipoJob;
import br.tec.facilitaservicos.scheduler.dominio.enums.PrioridadeJob;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ============================================================================
 * 📋 DTO PARA REPRESENTAÇÃO DE JOBS
 * ============================================================================
 * 
 * Data Transfer Object para jobs do scheduler com serialização otimizada.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public class JobDto {

    private String id;
    private String nome;
    private String descricao;
    private TipoJob tipo;
    private StatusJob status;
    private PrioridadeJob prioridade;
    private String grupo;
    private String cronExpression;
    private Map<String, Object> parametros;
    
    // Configurações de execução
    private Integer timeoutSegundos;
    private Integer maxTentativas;
    private Integer tentativas;
    private Integer maxExecucoesConcorrentes;
    private Integer execucoesAtivas;
    
    // Estatísticas
    private Long totalExecucoes;
    private Long totalSucessos;
    private Long totalFalhas;
    private String ultimoErro;
    private Long duracaoExecucaoMs;
    
    // Circuit breaker
    private Boolean circuitBreakerAberto;
    private Integer circuitBreakerFailures;
    
    // Timestamps
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime agendadoPara;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime iniciadoEm;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completadoEm;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime proximoRetry;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timeoutEm;
    
    private Boolean ativo;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime criadoEm;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime atualizadoEm;

    // Construtores
    public JobDto() {}

    public JobDto(String id, String nome, String descricao, TipoJob tipo, StatusJob status, 
                  PrioridadeJob prioridade, String grupo, String cronExpression, 
                  Map<String, Object> parametros, Integer timeoutSegundos, Integer maxTentativas, 
                  Integer tentativas, Integer maxExecucoesConcorrentes, Integer execucoesAtivas,
                  Long totalExecucoes, Long totalSucessos, Long totalFalhas, String ultimoErro,
                  Long duracaoExecucaoMs, Boolean circuitBreakerAberto, Integer circuitBreakerFailures,
                  LocalDateTime agendadoPara, LocalDateTime iniciadoEm, LocalDateTime completadoEm,
                  LocalDateTime proximoRetry, LocalDateTime timeoutEm, Boolean ativo,
                  LocalDateTime criadoEm, LocalDateTime atualizadoEm) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.status = status;
        this.prioridade = prioridade;
        this.grupo = grupo;
        this.cronExpression = cronExpression;
        this.parametros = parametros;
        this.timeoutSegundos = timeoutSegundos;
        this.maxTentativas = maxTentativas;
        this.tentativas = tentativas;
        this.maxExecucoesConcorrentes = maxExecucoesConcorrentes;
        this.execucoesAtivas = execucoesAtivas;
        this.totalExecucoes = totalExecucoes;
        this.totalSucessos = totalSucessos;
        this.totalFalhas = totalFalhas;
        this.ultimoErro = ultimoErro;
        this.duracaoExecucaoMs = duracaoExecucaoMs;
        this.circuitBreakerAberto = circuitBreakerAberto;
        this.circuitBreakerFailures = circuitBreakerFailures;
        this.agendadoPara = agendadoPara;
        this.iniciadoEm = iniciadoEm;
        this.completadoEm = completadoEm;
        this.proximoRetry = proximoRetry;
        this.timeoutEm = timeoutEm;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public TipoJob getTipo() { return tipo; }
    public void setTipo(TipoJob tipo) { this.tipo = tipo; }

    public StatusJob getStatus() { return status; }
    public void setStatus(StatusJob status) { this.status = status; }

    public PrioridadeJob getPrioridade() { return prioridade; }
    public void setPrioridade(PrioridadeJob prioridade) { this.prioridade = prioridade; }

    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public Map<String, Object> getParametros() { return parametros; }
    public void setParametros(Map<String, Object> parametros) { this.parametros = parametros; }

    public Integer getTimeoutSegundos() { return timeoutSegundos; }
    public void setTimeoutSegundos(Integer timeoutSegundos) { this.timeoutSegundos = timeoutSegundos; }

    public Integer getMaxTentativas() { return maxTentativas; }
    public void setMaxTentativas(Integer maxTentativas) { this.maxTentativas = maxTentativas; }

    public Integer getTentativas() { return tentativas; }
    public void setTentativas(Integer tentativas) { this.tentativas = tentativas; }

    public Integer getMaxExecucoesConcorrentes() { return maxExecucoesConcorrentes; }
    public void setMaxExecucoesConcorrentes(Integer maxExecucoesConcorrentes) { this.maxExecucoesConcorrentes = maxExecucoesConcorrentes; }

    public Integer getExecucoesAtivas() { return execucoesAtivas; }
    public void setExecucoesAtivas(Integer execucoesAtivas) { this.execucoesAtivas = execucoesAtivas; }

    public Long getTotalExecucoes() { return totalExecucoes; }
    public void setTotalExecucoes(Long totalExecucoes) { this.totalExecucoes = totalExecucoes; }

    public Long getTotalSucessos() { return totalSucessos; }
    public void setTotalSucessos(Long totalSucessos) { this.totalSucessos = totalSucessos; }

    public Long getTotalFalhas() { return totalFalhas; }
    public void setTotalFalhas(Long totalFalhas) { this.totalFalhas = totalFalhas; }

    public String getUltimoErro() { return ultimoErro; }
    public void setUltimoErro(String ultimoErro) { this.ultimoErro = ultimoErro; }

    public Long getDuracaoExecucaoMs() { return duracaoExecucaoMs; }
    public void setDuracaoExecucaoMs(Long duracaoExecucaoMs) { this.duracaoExecucaoMs = duracaoExecucaoMs; }

    public Boolean getCircuitBreakerAberto() { return circuitBreakerAberto; }
    public void setCircuitBreakerAberto(Boolean circuitBreakerAberto) { this.circuitBreakerAberto = circuitBreakerAberto; }

    public Integer getCircuitBreakerFailures() { return circuitBreakerFailures; }
    public void setCircuitBreakerFailures(Integer circuitBreakerFailures) { this.circuitBreakerFailures = circuitBreakerFailures; }

    public LocalDateTime getAgendadoPara() { return agendadoPara; }
    public void setAgendadoPara(LocalDateTime agendadoPara) { this.agendadoPara = agendadoPara; }

    public LocalDateTime getIniciadoEm() { return iniciadoEm; }
    public void setIniciadoEm(LocalDateTime iniciadoEm) { this.iniciadoEm = iniciadoEm; }

    public LocalDateTime getCompletadoEm() { return completadoEm; }
    public void setCompletadoEm(LocalDateTime completadoEm) { this.completadoEm = completadoEm; }

    public LocalDateTime getProximoRetry() { return proximoRetry; }
    public void setProximoRetry(LocalDateTime proximoRetry) { this.proximoRetry = proximoRetry; }

    public LocalDateTime getTimeoutEm() { return timeoutEm; }
    public void setTimeoutEm(LocalDateTime timeoutEm) { this.timeoutEm = timeoutEm; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    
    // Campos calculados
    public String getStatusDisplay() {
        return status != null ? status.toString() : "DESCONHECIDO";
    }
    
    public String getPrioridadeDisplay() {
        return prioridade != null ? prioridade.toString() : "NORMAL";
    }
    
    public boolean isEmExecucao() {
        return status != null && status.name().contains("EXECUTANDO");
    }
    
    public boolean isFinalizado() {
        return status != null && (status.name().equals("CONCLUIDO") || status.name().equals("FALHA"));
    }
    
    public double getTaxaSucesso() {
        if (totalExecucoes == null || totalExecucoes == 0) return 0.0;
        if (totalSucessos == null) return 0.0;
        return (totalSucessos.doubleValue() / totalExecucoes.doubleValue()) * 100.0;
    }
    
    public String getDuracaoFormatada() {
        if (duracaoExecucaoMs == null || duracaoExecucaoMs == 0) return "N/A";
        
        long millis = duracaoExecucaoMs;
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