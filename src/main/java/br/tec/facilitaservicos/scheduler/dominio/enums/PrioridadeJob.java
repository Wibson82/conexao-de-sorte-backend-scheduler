package br.tec.facilitaservicos.scheduler.dominio.enums;

/**
 * ============================================================================
 * 🎯 PRIORIDADES DE JOBS PARA CONTROLE DE EXECUÇÃO
 * ============================================================================
 * 
 * Enum que define os níveis de prioridade para jobs no sistema de scheduler:
 * - EMERGENCIA: Jobs críticos que devem ser executados imediatamente
 * - CRITICA: Jobs importantes que não podem falhar
 * - ALTA: Jobs que devem ser executados rapidamente
 * - NORMAL: Jobs de rotina com prioridade padrão
 * - BAIXA: Jobs que podem aguardar slots disponíveis
 * 
 * Cada prioridade influencia:
 * - Ordem de execução na fila
 * - Recursos alocados (CPU/Memory)
 * - Timeout configurado
 * - Política de retry
 * - Alertas e notificações
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public enum PrioridadeJob {

    /**
     * Emergência - Execução imediata obrigatória
     * - Sistema em risco ou falha crítica
     * - Dados sendo corrompidos
     * - Segurança comprometida
     */
    EMERGENCIA(100, "Emergência", "#721c24", 0, 10, 3600, true, true),

    /**
     * Crítica - Alta prioridade, não pode falhar
     * - Processos essenciais do negócio
     * - ETL de dados críticos
     * - Backups essenciais
     */
    CRITICA(80, "Crítica", "#dc3545", 30, 7, 1800, true, true),

    /**
     * Alta - Deve ser executado rapidamente
     * - Relatórios importantes
     * - Processamento urgente
     * - Integrações prioritárias
     */
    ALTA(60, "Alta", "#fd7e14", 300, 5, 900, true, false),

    /**
     * Normal - Prioridade padrão
     * - Jobs de rotina
     * - Processamento regular
     * - Manutenção programada
     */
    NORMAL(40, "Normal", "#28a745", 600, 3, 600, false, false),

    /**
     * Baixa - Pode aguardar recursos disponíveis
     * - Relatórios não urgentes
     * - Limpeza de dados antigos
     * - Otimizações
     */
    BAIXA(20, "Baixa", "#6c757d", 1800, 2, 300, false, false);

    private final int peso;
    private final String nome;
    private final String cor;
    private final int delayMaximoSegundos;
    private final int maxTentativas;
    private final int timeoutSegundos;
    private final boolean notificacaoImediata;
    private final boolean preemptivo;

    PrioridadeJob(int peso, String nome, String cor, int delayMaximoSegundos,
                  int maxTentativas, int timeoutSegundos, boolean notificacaoImediata, boolean preemptivo) {
        this.peso = peso;
        this.nome = nome;
        this.cor = cor;
        this.delayMaximoSegundos = delayMaximoSegundos;
        this.maxTentativas = maxTentativas;
        this.timeoutSegundos = timeoutSegundos;
        this.notificacaoImediata = notificacaoImediata;
        this.preemptivo = preemptivo;
    }

    // === MÉTODOS DE CLASSIFICAÇÃO ===

    /**
     * Verifica se é uma prioridade crítica ou superior
     */
    public boolean isCriticaOuSuperior() {
        return this.peso >= CRITICA.peso;
    }

    /**
     * Verifica se é prioridade de emergência
     */
    public boolean isEmergencia() {
        return this == EMERGENCIA;
    }

    /**
     * Verifica se pode interromper jobs de menor prioridade
     */
    public boolean isPreemptivo() {
        return preemptivo;
    }

    /**
     * Verifica se requer notificação imediata
     */
    public boolean requerNotificacaoImediata() {
        return notificacaoImediata;
    }

    /**
     * Verifica se deve ser executado em pool dedicado
     */
    public boolean deveUsarPoolDedicado() {
        return this.peso >= ALTA.peso;
    }

    // === MÉTODOS DE COMPARAÇÃO ===

    /**
     * Verifica se esta prioridade é maior que outra
     */
    public boolean isMaiorQue(PrioridadeJob outra) {
        return this.peso > outra.peso;
    }

    /**
     * Verifica se esta prioridade é menor que outra
     */
    public boolean isMenorQue(PrioridadeJob outra) {
        return this.peso < outra.peso;
    }

    /**
     * Retorna a prioridade mais alta entre duas
     */
    public PrioridadeJob max(PrioridadeJob outra) {
        return this.peso >= outra.peso ? this : outra;
    }

    /**
     * Retorna a prioridade mais baixa entre duas
     */
    public PrioridadeJob min(PrioridadeJob outra) {
        return this.peso <= outra.peso ? this : outra;
    }

    // === CONFIGURAÇÕES DE RECURSOS ===

    /**
     * Obtém o número de threads recomendado para esta prioridade
     */
    public int getThreadsRecomendadas() {
        return switch (this) {
            case EMERGENCIA -> 10; // Pool dedicado grande
            case CRITICA -> 5;     // Pool dedicado médio
            case ALTA -> 3;        // Pool compartilhado grande
            case NORMAL -> 2;      // Pool compartilhado médio
            case BAIXA -> 1;       // Pool compartilhado pequeno
        };
    }

    /**
     * Obtém a quantidade de memória recomendada (em MB)
     */
    public int getMemoriaRecomendadaMB() {
        return switch (this) {
            case EMERGENCIA -> 2048; // 2GB
            case CRITICA -> 1024;    // 1GB
            case ALTA -> 512;        // 512MB
            case NORMAL -> 256;      // 256MB
            case BAIXA -> 128;       // 128MB
        };
    }

    /**
     * Obtém o percentual de CPU recomendado
     */
    public double getCpuPercentualRecomendado() {
        return switch (this) {
            case EMERGENCIA -> 1.0;   // 100% CPU disponível
            case CRITICA -> 0.8;      // 80% CPU
            case ALTA -> 0.6;         // 60% CPU
            case NORMAL -> 0.4;       // 40% CPU
            case BAIXA -> 0.2;        // 20% CPU
        };
    }

    // === CONFIGURAÇÕES DE EXECUÇÃO ===

    /**
     * Obtém o intervalo entre tentativas de retry (segundos)
     */
    public int getIntervaloRetrySegundos() {
        return switch (this) {
            case EMERGENCIA -> 10;  // Retry muito rápido
            case CRITICA -> 30;     // Retry rápido
            case ALTA -> 60;        // Retry normal
            case NORMAL -> 300;     // Retry lento
            case BAIXA -> 600;      // Retry muito lento
        };
    }

    /**
     * Verifica se deve usar estratégia de retry exponencial
     */
    public boolean usarRetryExponencial() {
        return this.peso <= NORMAL.peso;
    }

    /**
     * Obtém o fator multiplicador para retry exponencial
     */
    public double getFatorRetryExponencial() {
        return switch (this) {
            case EMERGENCIA, CRITICA -> 1.5; // Crescimento lento
            case ALTA -> 2.0;                // Crescimento moderado
            case NORMAL -> 2.5;              // Crescimento padrão
            case BAIXA -> 3.0;               // Crescimento rápido
        };
    }

    // === CONFIGURAÇÕES DE MONITORAMENTO ===

    /**
     * Obtém o intervalo de health check (segundos)
     */
    public int getIntervaloHealthCheckSegundos() {
        return switch (this) {
            case EMERGENCIA -> 5;   // Monitoramento contínuo
            case CRITICA -> 15;     // Monitoramento frequente
            case ALTA -> 30;        // Monitoramento regular
            case NORMAL -> 60;      // Monitoramento espaçado
            case BAIXA -> 300;      // Monitoramento mínimo
        };
    }

    /**
     * Verifica se deve manter log detalhado
     */
    public boolean manterLogDetalhado() {
        return this.peso >= ALTA.peso;
    }

    /**
     * Obtém o nível de log apropriado
     */
    public String getNivelLogPadrao() {
        return switch (this) {
            case EMERGENCIA, CRITICA -> "DEBUG"; // Log completo
            case ALTA -> "INFO";                 // Log informativo
            case NORMAL -> "WARN";               // Apenas warnings
            case BAIXA -> "ERROR";               // Apenas erros
        };
    }

    // === CONFIGURAÇÕES DE ALERTAS ===

    /**
     * Obtém os canais de notificação para esta prioridade
     */
    public String[] getCanaisNotificacao() {
        return switch (this) {
            case EMERGENCIA -> new String[]{"email", "slack", "sms", "phone", "teams"};
            case CRITICA -> new String[]{"email", "slack", "sms", "teams"};
            case ALTA -> new String[]{"email", "slack", "teams"};
            case NORMAL -> new String[]{"email", "slack"};
            case BAIXA -> new String[]{"email"};
        };
    }

    /**
     * Verifica se deve escalar automaticamente
     */
    public boolean deveEscalarAutomaticamente() {
        return this.peso >= CRITICA.peso;
    }

    /**
     * Obtém o tempo para escalation (minutos)
     */
    public int getTempoEscalationMinutos() {
        return switch (this) {
            case EMERGENCIA -> 5;   // Escalation imediato
            case CRITICA -> 15;     // Escalation rápido
            case ALTA -> 60;        // Escalation em 1 hora
            case NORMAL -> 240;     // Escalation em 4 horas
            case BAIXA -> 1440;     // Escalation em 24 horas
        };
    }

    // === CONFIGURAÇÕES DE AGENDAMENTO ===

    /**
     * Verifica se pode ser executado fora do horário comercial
     */
    public boolean podeExecutarForaHorarioComercial() {
        return this == EMERGENCIA || this == CRITICA || this == BAIXA;
    }

    /**
     * Verifica se deve aguardar horário de baixo uso
     */
    public boolean aguardarHorarioBaixoUso() {
        return this == BAIXA;
    }

    /**
     * Obtém a janela preferencial de execução (hora do dia)
     */
    public int[] getJanelaPreferencialHoras() {
        return switch (this) {
            case EMERGENCIA -> new int[]{0, 23}; // Qualquer hora
            case CRITICA -> new int[]{0, 23};    // Qualquer hora
            case ALTA -> new int[]{6, 22};       // Horário estendido
            case NORMAL -> new int[]{8, 18};     // Horário comercial
            case BAIXA -> new int[]{22, 6};      // Madrugada
        };
    }

    // === MÉTODOS UTILITÁRIOS ===

    /**
     * Busca prioridade por peso numérico
     */
    public static PrioridadeJob porPeso(int peso) {
        PrioridadeJob maisProxima = NORMAL;
        int menorDistancia = Math.abs(NORMAL.peso - peso);
        
        for (PrioridadeJob prioridade : values()) {
            int distancia = Math.abs(prioridade.peso - peso);
            if (distancia < menorDistancia) {
                menorDistancia = distancia;
                maisProxima = prioridade;
            }
        }
        
        return maisProxima;
    }

    /**
     * Busca prioridade por nome
     */
    public static PrioridadeJob porNome(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return NORMAL;
        }
        
        try {
            return valueOf(nome.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Tentar nomes alternativos
            String upper = nome.toUpperCase();
            return switch (upper) {
                case "EMERGENCY", "URGENT", "CRITICAL_HIGH" -> EMERGENCIA;
                case "CRITICAL", "CRIT", "HIGH_PRIORITY" -> CRITICA;
                case "HIGH", "IMPORTANT" -> ALTA;
                case "MEDIUM", "STANDARD", "DEFAULT" -> NORMAL;
                case "LOW", "BACKGROUND" -> BAIXA;
                default -> NORMAL;
            };
        }
    }

    /**
     * Obtém emoji representativo da prioridade
     */
    public String getEmoji() {
        return switch (this) {
            case EMERGENCIA -> "🚨";
            case CRITICA -> "🔥";
            case ALTA -> "⚡";
            case NORMAL -> "📋";
            case BAIXA -> "⏳";
        };
    }

    /**
     * Obtém todas as prioridades críticas
     */
    public static PrioridadeJob[] getPrioridadesCriticas() {
        return new PrioridadeJob[]{EMERGENCIA, CRITICA};
    }

    /**
     * Obtém todas as prioridades altas
     */
    public static PrioridadeJob[] getPrioridadesAltas() {
        return new PrioridadeJob[]{EMERGENCIA, CRITICA, ALTA};
    }

    // === GETTERS ===

    public int getPeso() {
        return peso;
    }

    public String getNome() {
        return nome;
    }

    public String getCor() {
        return cor;
    }

    public int getDelayMaximoSegundos() {
        return delayMaximoSegundos;
    }

    public int getMaxTentativas() {
        return maxTentativas;
    }

    public int getTimeoutSegundos() {
        return timeoutSegundos;
    }

    public boolean isNotificacaoImediata() {
        return notificacaoImediata;
    }

    @Override
    public String toString() {
        return String.format("%s %s (Peso: %d)", getEmoji(), nome, peso);
    }
}