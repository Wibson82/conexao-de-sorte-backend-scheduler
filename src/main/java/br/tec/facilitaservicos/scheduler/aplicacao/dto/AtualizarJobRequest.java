package br.tec.facilitaservicos.scheduler.aplicacao.dto;

import br.tec.facilitaservicos.scheduler.dominio.enums.PrioridadeJob;
import jakarta.validation.constraints.*;

import java.util.Map;

/**
 * ============================================================================
 * 📝 REQUEST PARA ATUALIZAR JOBS
 * ============================================================================
 * 
 * DTO para atualização de jobs com validações.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public class AtualizarJobRequest {

    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String nome;
    
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String descricao;
    
    private PrioridadeJob prioridade;
    
    @Pattern(regexp = "^\\s*(\\S+\\s+){5}\\S+\\s*$", 
             message = "Expressão CRON deve ter formato válido (6 campos)")
    private String cronExpression;
    
    private Map<String, Object> parametros;
    
    @Min(value = 1, message = "Timeout deve ser pelo menos 1 segundo")
    @Max(value = 86400, message = "Timeout não pode exceder 24 horas (86400 segundos)")
    private Integer timeoutSegundos;
    
    @Min(value = 1, message = "Máximo de tentativas deve ser pelo menos 1")
    @Max(value = 10, message = "Máximo de tentativas não pode exceder 10")
    private Integer maxTentativas;
    
    private Boolean ativo;

    // Construtores
    public AtualizarJobRequest() {}

    public AtualizarJobRequest(String nome, String descricao, PrioridadeJob prioridade,
                              String cronExpression, Map<String, Object> parametros,
                              Integer timeoutSegundos, Integer maxTentativas, Boolean ativo) {
        this.nome = nome;
        this.descricao = descricao;
        this.prioridade = prioridade;
        this.cronExpression = cronExpression;
        this.parametros = parametros;
        this.timeoutSegundos = timeoutSegundos;
        this.maxTentativas = maxTentativas;
        this.ativo = ativo;
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public PrioridadeJob getPrioridade() { return prioridade; }
    public void setPrioridade(PrioridadeJob prioridade) { this.prioridade = prioridade; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public Map<String, Object> getParametros() { return parametros; }
    public void setParametros(Map<String, Object> parametros) { this.parametros = parametros; }

    public Integer getTimeoutSegundos() { return timeoutSegundos; }
    public void setTimeoutSegundos(Integer timeoutSegundos) { this.timeoutSegundos = timeoutSegundos; }

    public Integer getMaxTentativas() { return maxTentativas; }
    public void setMaxTentativas(Integer maxTentativas) { this.maxTentativas = maxTentativas; }

    public Boolean getAtivo() { return ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}