package br.tec.facilitaservicos.scheduler.dominio.enums;

/**
 * ============================================================================
 * 📊 STATUS DE JOBS PARA CONTROLE DE EXECUÇÃO
 * ============================================================================
 * 
 * Enum que define os diferentes estados de um job no sistema de scheduler:
 * - Estados de ciclo de vida (criado, pronto, executando, concluído)
 * - Estados de erro (falhado, timeout, circuit breaker)
 * - Estados de controle (pausado, cancelado, desativado)
 * - Estados de dependência (aguardando, bloqueado)
 * 
 * Cada status tem comportamentos específicos:
 * - Se pode ser executado
 * - Se pode ser cancelado
 * - Se pode ser reagendado
 * - Ações automáticas
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum StatusJob {

    // === ESTADOS INICIAIS ===
    
    /**
     * Job criado mas ainda não agendado
     */
    CRIADO(0, "Criado", "#6c757d", true, true, false, false),
    
    /**
     * Job pronto para execução, aguardando scheduler
     */
    PRONTO(1, "Pronto", "#17a2b8", true, true, false, false),
    
    /**
     * Job agendado para execução futura
     */
    AGENDADO(2, "Agendado", "#007bff", true, true, false, false),

    // === ESTADOS DE EXECUÇÃO ===
    
    /**
     * Job está sendo executado no momento
     */
    EXECUTANDO(3, "Executando", "#ffc107", false, true, true, false),
    
    /**
     * Job executado com sucesso
     */
    EXECUTADO(4, "Executado", "#28a745", false, false, false, true),
    
    /**
     * Job completado (executado com sucesso e finalizações feitas)
     */
    COMPLETADO(5, "Completado", "#28a745", false, false, false, true),

    // === ESTADOS DE ERRO ===
    
    /**
     * Job falhou durante a execução
     */
    FALHADO(10, "Falhado", "#dc3545", true, true, false, false),
    
    /**
     * Job excedeu o tempo limite de execução
     */
    TIMEOUT(11, "Timeout", "#fd7e14", true, true, false, false),
    
    /**
     * Job foi cancelado pelo usuário ou sistema
     */
    CANCELADO(12, "Cancelado", "#6f42c1", false, false, false, false),
    
    /**
     * Job com muitas falhas, circuit breaker ativado
     */
    CIRCUIT_BREAKER_ABERTO(13, "Circuit Breaker", "#e83e8c", false, true, false, false),

    // === ESTADOS DE CONTROLE ===
    
    /**
     * Job pausado temporariamente
     */
    PAUSADO(20, "Pausado", "#fd7e14", true, true, false, false),
    
    /**
     * Job desativado permanentemente
     */
    DESATIVADO(21, "Desativado", "#6c757d", false, false, false, false),
    
    /**
     * Job arquivado (não será mais executado)
     */
    ARQUIVADO(22, "Arquivado", "#495057", false, false, false, false),

    // === ESTADOS DE DEPENDÊNCIA ===
    
    /**
     * Job aguardando dependências serem satisfeitas
     */
    AGUARDANDO_DEPENDENCIAS(30, "Aguardando Dependências", "#17a2b8", false, true, false, false),
    
    /**
     * Job bloqueado por outro job ou recurso
     */
    BLOQUEADO(31, "Bloqueado", "#e83e8c", false, true, false, false),
    
    /**
     * Job aguardando slot disponível para execução
     */
    AGUARDANDO_SLOT(32, "Aguardando Slot", "#20c997", false, true, false, false),

    // === ESTADOS ESPECIAIS ===
    
    /**
     * Job sendo executado novamente (retry)
     */
    REEXECUTANDO(40, "Re-executando", "#fd7e14", false, true, true, false),
    
    /**
     * Job foi interrompido mas pode ser retomado
     */
    INTERROMPIDO(41, "Interrompido", "#6f42c1", true, true, false, false),
    
    /**
     * Job está sendo processado (pós-execução)
     */
    PROCESSANDO(42, "Processando", "#20c997", false, false, true, false);

    private final int codigo;
    private final String nome;
    private final String cor;
    private final boolean podeExecutar;
    private final boolean podeCancelar;
    private final boolean emExecucao;
    private final boolean finalizado;

    StatusJob(int codigo, String nome, String cor, boolean podeExecutar, 
              boolean podeCancelar, boolean emExecucao, boolean finalizado) {
        this.codigo = codigo;
        this.nome = nome;
        this.cor = cor;
        this.podeExecutar = podeExecutar;
        this.podeCancelar = podeCancelar;
        this.emExecucao = emExecucao;
        this.finalizado = finalizado;
    }

    // === MÉTODOS DE CLASSIFICAÇÃO ===

    /**
     * Verifica se é um estado inicial (antes da execução)
     */
    public boolean isInicial() {
        return this == CRIADO || this == PRONTO || this == AGENDADO;
    }

    /**
     * Verifica se é um estado de execução
     */
    public boolean isExecucao() {
        return emExecucao;
    }

    /**
     * Verifica se é um estado de sucesso
     */
    public boolean isSucesso() {
        return this == EXECUTADO || this == COMPLETADO;
    }

    /**
     * Verifica se é um estado de erro
     */
    public boolean isErro() {
        return this == FALHADO || this == TIMEOUT || this == CIRCUIT_BREAKER_ABERTO;
    }

    /**
     * Verifica se é um estado de controle (pausado/desativado)
     */
    public boolean isControle() {
        return this == PAUSADO || this == DESATIVADO || this == ARQUIVADO;
    }

    /**
     * Verifica se é um estado de aguardo
     */
    public boolean isAguardo() {
        return this == AGUARDANDO_DEPENDENCIAS || this == AGUARDANDO_SLOT || this == BLOQUEADO;
    }

    /**
     * Verifica se o job pode ser reagendado
     */
    public boolean podeReagendar() {
        return this != EXECUTANDO && this != PROCESSANDO && this != DESATIVADO && this != ARQUIVADO;
    }

    /**
     * Verifica se o job pode ser editado
     */
    public boolean podeEditar() {
        return !emExecucao && !finalizado;
    }

    /**
     * Verifica se o job pode ser removido
     */
    public boolean podeRemover() {
        return !emExecucao;
    }

    // === TRANSIÇÕES DE ESTADO ===

    /**
     * Obtém os próximos estados possíveis a partir deste estado
     */
    public StatusJob[] getProximosEstadosPossiveis() {
        return switch (this) {
            case CRIADO -> new StatusJob[]{PRONTO, AGENDADO, PAUSADO, DESATIVADO};
            case PRONTO -> new StatusJob[]{EXECUTANDO, AGUARDANDO_DEPENDENCIAS, AGUARDANDO_SLOT, PAUSADO, CANCELADO};
            case AGENDADO -> new StatusJob[]{PRONTO, EXECUTANDO, PAUSADO, CANCELADO};
            case EXECUTANDO -> new StatusJob[]{EXECUTADO, FALHADO, TIMEOUT, CANCELADO, PROCESSANDO};
            case EXECUTADO -> new StatusJob[]{COMPLETADO, PROCESSANDO};
            case FALHADO -> new StatusJob[]{PRONTO, REEXECUTANDO, CIRCUIT_BREAKER_ABERTO, DESATIVADO};
            case TIMEOUT -> new StatusJob[]{PRONTO, REEXECUTANDO, DESATIVADO};
            case PAUSADO -> new StatusJob[]{PRONTO, AGENDADO, DESATIVADO};
            case REEXECUTANDO -> new StatusJob[]{EXECUTADO, FALHADO, TIMEOUT, CANCELADO};
            case INTERROMPIDO -> new StatusJob[]{PRONTO, REEXECUTANDO, CANCELADO};
            case CIRCUIT_BREAKER_ABERTO -> new StatusJob[]{PRONTO, DESATIVADO};
            case AGUARDANDO_DEPENDENCIAS -> new StatusJob[]{PRONTO, BLOQUEADO, TIMEOUT, CANCELADO};
            case BLOQUEADO -> new StatusJob[]{PRONTO, AGUARDANDO_SLOT, TIMEOUT, CANCELADO};
            case AGUARDANDO_SLOT -> new StatusJob[]{EXECUTANDO, BLOQUEADO, TIMEOUT, CANCELADO};
            case PROCESSANDO -> new StatusJob[]{COMPLETADO, FALHADO};
            default -> new StatusJob[]{};
        };
    }

    /**
     * Verifica se pode transicionar para outro estado
     */
    public boolean podeTransicionarPara(StatusJob novoStatus) {
        StatusJob[] possiveis = getProximosEstadosPossiveis();
        for (StatusJob status : possiveis) {
            if (status == novoStatus) {
                return true;
            }
        }
        return false;
    }

    // === CONFIGURAÇÕES POR STATUS ===

    /**
     * Obtém o timeout padrão para este status (em segundos)
     */
    public int getTimeoutPadrao() {
        return switch (this) {
            case EXECUTANDO -> 300; // 5 minutos
            case REEXECUTANDO -> 600; // 10 minutos (mais tempo para retry)
            case PROCESSANDO -> 120; // 2 minutos
            case AGUARDANDO_DEPENDENCIAS -> 1800; // 30 minutos
            case AGUARDANDO_SLOT -> 900; // 15 minutos
            case BLOQUEADO -> 600; // 10 minutos
            default -> 0; // Sem timeout
        };
    }

    /**
     * Obtém a prioridade de processamento baseada no status
     */
    public int getPrioridadeProcessamento() {
        return switch (this) {
            case EXECUTANDO, REEXECUTANDO -> 100; // Máxima prioridade
            case PRONTO -> 80;
            case AGENDADO -> 60;
            case FALHADO, TIMEOUT -> 40; // Menor prioridade para retry
            case CIRCUIT_BREAKER_ABERTO -> 20; // Mínima prioridade
            default -> 50; // Prioridade média
        };
    }

    /**
     * Verifica se deve notificar sobre mudança de status
     */
    public boolean deveNotificar() {
        return this == FALHADO || this == TIMEOUT || this == CIRCUIT_BREAKER_ABERTO || 
               this == COMPLETADO;
    }

    /**
     * Obtém o nível de log apropriado para este status
     */
    public String getNivelLog() {
        return switch (this) {
            case EXECUTANDO, REEXECUTANDO, PROCESSANDO -> "INFO";
            case EXECUTADO, COMPLETADO -> "INFO";
            case FALHADO, TIMEOUT -> "ERROR";
            case CIRCUIT_BREAKER_ABERTO -> "WARN";
            case CANCELADO, PAUSADO -> "WARN";
            default -> "DEBUG";
        };
    }

    // === MÉTRICAS E ESTATÍSTICAS ===

    /**
     * Verifica se deve ser contado nas estatísticas de sucesso
     */
    public boolean contaComoSucesso() {
        return this == EXECUTADO || this == COMPLETADO;
    }

    /**
     * Verifica se deve ser contado nas estatísticas de falha
     */
    public boolean contaComoFalha() {
        return this == FALHADO || this == TIMEOUT;
    }

    /**
     * Verifica se deve ser contado como execução
     */
    public boolean contaComoExecucao() {
        return contaComoSucesso() || contaComoFalha();
    }

    // === MÉTODOS UTILITÁRIOS ===

    /**
     * Busca status por código numérico
     */
    public static StatusJob porCodigo(int codigo) {
        for (StatusJob status : values()) {
            if (status.codigo == codigo) {
                return status;
            }
        }
        return CRIADO; // Fallback
    }

    /**
     * Busca status por nome
     */
    public static StatusJob porNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return CRIADO;
        }
        
        try {
            return valueOf(nome.toUpperCase().replace(" ", "_"));
        } catch (IllegalArgumentException e) {
            // Tentar nomes alternativos
            String upper = nome.toUpperCase();
            return switch (upper) {
                case "RUNNING", "EXECUTING" -> EXECUTANDO;
                case "COMPLETED", "DONE", "SUCCESS" -> COMPLETADO;
                case "FAILED", "ERROR", "FAILURE" -> FALHADO;
                case "CANCELLED", "CANCELED" -> CANCELADO;
                case "PAUSED", "SUSPENDED" -> PAUSADO;
                case "DISABLED", "INACTIVE" -> DESATIVADO;
                default -> CRIADO;
            };
        }
    }

    /**
     * Obtém emoji representativo do status
     */
    public String getEmoji() {
        return switch (this) {
            case CRIADO -> "🆕";
            case PRONTO -> "✅";
            case AGENDADO -> "⏰";
            case EXECUTANDO -> "🔄";
            case EXECUTADO, COMPLETADO -> "✅";
            case FALHADO -> "❌";
            case TIMEOUT -> "⏱️";
            case CANCELADO -> "🚫";
            case PAUSADO -> "⏸️";
            case DESATIVADO -> "🔇";
            case CIRCUIT_BREAKER_ABERTO -> "🚨";
            case AGUARDANDO_DEPENDENCIAS -> "⏳";
            case BLOQUEADO -> "🔒";
            case REEXECUTANDO -> "🔄";
            case INTERROMPIDO -> "⏹️";
            case PROCESSANDO -> "⚙️";
            default -> "❓";
        };
    }

    /**
     * Obtém todos os status ativos (que podem ser executados)
     */
    public static StatusJob[] getStatusAtivos() {
        return java.util.Arrays.stream(values())
            .filter(status -> status.podeExecutar)
            .toArray(StatusJob[]::new);
    }

    /**
     * Obtém todos os status finalizados
     */
    public static StatusJob[] getStatusFinalizados() {
        return java.util.Arrays.stream(values())
            .filter(status -> status.finalizado)
            .toArray(StatusJob[]::new);
    }

    // === GETTERS ===

    public int getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getCor() {
        return cor;
    }

    public boolean isPodeExecutar() {
        return podeExecutar;
    }

    public boolean isPodeCancelar() {
        return podeCancelar;
    }

    public boolean isEmExecucao() {
        return emExecucao;
    }

    public boolean isFinalizado() {
        return finalizado;
    }

    @Override
    public String toString() {
        return String.format("%s %s (%d)", getEmoji(), nome, codigo);
    }
}