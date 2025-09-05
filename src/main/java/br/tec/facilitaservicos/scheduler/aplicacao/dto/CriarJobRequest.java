package br.tec.facilitaservicos.scheduler.aplicacao.dto;

import br.tec.facilitaservicos.scheduler.dominio.enums.TipoJob;
import br.tec.facilitaservicos.scheduler.dominio.enums.PrioridadeJob;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ============================================================================
 * 🆕 REQUEST PARA CRIAR NOVOS JOBS
 * ============================================================================
 * 
 * DTO para criação de jobs com validações completas.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public class CriarJobRequest {

    @NotBlank(message = "Nome do job é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String nome;
    
    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String descricao;
    
    @NotNull(message = "Tipo do job é obrigatório")
    private TipoJob tipo;
    
    @NotNull(message = "Prioridade do job é obrigatória")
    private PrioridadeJob prioridade;
    
    @NotBlank(message = "Grupo do job é obrigatório")
    @Size(min = 2, max = 50, message = "Grupo deve ter entre 2 e 50 caracteres")
    private String grupo;
    
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
    
    @Min(value = 1, message = "Máximo de execuções concorrentes deve ser pelo menos 1")
    @Max(value = 100, message = "Máximo de execuções concorrentes não pode exceder 100")
    private Integer maxExecucoesConcorrentes;
    
    @Future(message = "Data de agendamento deve ser no futuro")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime agendadoPara;

    // Construtores
    public CriarJobRequest() {}

    public CriarJobRequest(String nome, String descricao, TipoJob tipo, PrioridadeJob prioridade,
                          String grupo, String cronExpression, Map<String, Object> parametros,
                          Integer timeoutSegundos, Integer maxTentativas, Integer maxExecucoesConcorrentes,
                          LocalDateTime agendadoPara) {
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.prioridade = prioridade;
        this.grupo = grupo;
        this.cronExpression = cronExpression;
        this.parametros = parametros;
        this.timeoutSegundos = timeoutSegundos;
        this.maxTentativas = maxTentativas;
        this.maxExecucoesConcorrentes = maxExecucoesConcorrentes;
        this.agendadoPara = agendadoPara;
    }

    // Getters e Setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public TipoJob getTipo() { return tipo; }
    public void setTipo(TipoJob tipo) { this.tipo = tipo; }

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

    public Integer getMaxExecucoesConcorrentes() { return maxExecucoesConcorrentes; }
    public void setMaxExecucoesConcorrentes(Integer maxExecucoesConcorrentes) { this.maxExecucoesConcorrentes = maxExecucoesConcorrentes; }

    public LocalDateTime getAgendadoPara() { return agendadoPara; }
    public void setAgendadoPara(LocalDateTime agendadoPara) { this.agendadoPara = agendadoPara; }
    
    // Validação customizada
    @AssertTrue(message = "Job deve ter CRON expression OU data de agendamento")
    public boolean isAgendamentoValido() {
        return (cronExpression != null && !cronExpression.trim().isEmpty()) ||
               agendadoPara != null;
    }
}