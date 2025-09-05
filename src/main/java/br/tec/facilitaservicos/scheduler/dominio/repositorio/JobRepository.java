package br.tec.facilitaservicos.scheduler.dominio.repositorio;

import br.tec.facilitaservicos.scheduler.dominio.entidade.JobR2dbc;
import br.tec.facilitaservicos.scheduler.dominio.enums.StatusJob;
import br.tec.facilitaservicos.scheduler.dominio.enums.TipoJob;
import br.tec.facilitaservicos.scheduler.dominio.enums.PrioridadeJob;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * ============================================================================
 * 🗄️ REPOSITÓRIO REATIVO PARA JOBS
 * ============================================================================
 * 
 * Repositório R2DBC para operações reativas com jobs do scheduler.
 * Fornece consultas otimizadas para:
 * 
 * 🔍 CONSULTAS ESPECIALIZADAS:
 * - Jobs por status e prioridade
 * - Jobs prontos para execução
 * - Jobs em circuit breaker
 * - Estatísticas de execução
 * - Métricas de performance
 * 
 * ⚡ PERFORMANCE:
 * - Queries otimizadas com índices
 * - Paginação reativa
 * - Agregações eficientes
 * - Cache de consultas frequentes
 * 
 * 🛡️ CONSISTÊNCIA:
 * - Transações reativas
 * - Locks otimistas
 * - Validações de integridade
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Repository
public interface JobRepository extends R2dbcRepository<JobR2dbc, String> {

    // === CONSULTAS POR STATUS ===

    /**
     * Busca jobs por status específico
     */
    Flux<JobR2dbc> findByStatusOrderByPrioridadePesoDescCriadoEmAsc(StatusJob status);

    /**
     * Busca jobs prontos para execução
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status IN ('PRONTO', 'AGENDADO') 
        AND (agendado_para IS NULL OR agendado_para <= :agora)
        AND circuit_breaker_aberto = false
        AND (max_execucoes_concorrentes IS NULL OR execucoes_ativas < max_execucoes_concorrentes)
        ORDER BY prioridade_peso DESC, criado_em ASC
        LIMIT :limite
    """)
    Flux<JobR2dbc> findJobsProntosParaExecucao(LocalDateTime agora, int limite);

    /**
     * Busca jobs em execução
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status IN ('EXECUTANDO', 'REEXECUTANDO', 'PROCESSANDO')
        ORDER BY iniciado_em ASC
    """)
    Flux<JobR2dbc> findJobsEmExecucao();

    /**
     * Busca jobs com timeout
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status IN ('EXECUTANDO', 'REEXECUTANDO', 'PROCESSANDO')
        AND timeout_em < :agora
        ORDER BY timeout_em ASC
    """)
    Flux<JobR2dbc> findJobsComTimeout(LocalDateTime agora);

    // === CONSULTAS POR TIPO E GRUPO ===

    /**
     * Busca jobs por tipo
     */
    Flux<JobR2dbc> findByTipoOrderByCriadoEmDesc(TipoJob tipo);

    /**
     * Busca jobs por grupo
     */
    Flux<JobR2dbc> findByGrupoOrderByCriadoEmDesc(String grupo);

    /**
     * Busca jobs ativos por grupo
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE grupo = :grupo 
        AND status NOT IN ('COMPLETADO', 'CANCELADO', 'DESATIVADO', 'ARQUIVADO')
        ORDER BY prioridade_peso DESC, criado_em ASC
    """)
    Flux<JobR2dbc> findJobsAtivosPorGrupo(String grupo);

    // === CONSULTAS POR PRIORIDADE ===

    /**
     * Busca jobs por prioridade
     */
    Flux<JobR2dbc> findByPrioridadeOrderByCriadoEmDesc(PrioridadeJob prioridade);

    /**
     * Busca jobs de alta prioridade prontos
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status IN ('PRONTO', 'AGENDADO')
        AND prioridade_peso >= :pesoMinimo
        AND circuit_breaker_aberto = false
        ORDER BY prioridade_peso DESC, criado_em ASC
        LIMIT :limite
    """)
    Flux<JobR2dbc> findJobsAltaPrioridadeProntos(int pesoMinimo, int limite);

    // === CONSULTAS DE RETRY E CIRCUIT BREAKER ===

    /**
     * Busca jobs para retry
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status IN ('FALHADO', 'TIMEOUT', 'INTERROMPIDO')
        AND tentativas < max_tentativas
        AND circuit_breaker_aberto = false
        AND proximo_retry <= :agora
        ORDER BY prioridade_peso DESC, proximo_retry ASC
        LIMIT :limite
    """)
    Flux<JobR2dbc> findJobsParaRetry(LocalDateTime agora, int limite);

    /**
     * Busca jobs com circuit breaker aberto
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE circuit_breaker_aberto = true
        AND circuit_breaker_aberto_em < :tempoLimite
        ORDER BY circuit_breaker_aberto_em ASC
    """)
    Flux<JobR2dbc> findJobsCircuitBreakerParaReset(LocalDateTime tempoLimite);

    /**
     * Conta jobs com circuit breaker por tipo
     */
    @Query("SELECT COUNT(*) FROM jobs WHERE tipo = :tipo AND circuit_breaker_aberto = true")
    Mono<Long> countJobsCircuitBreakerPorTipo(TipoJob tipo);

    // === CONSULTAS DE DEPENDÊNCIAS ===

    /**
     * Busca jobs aguardando dependências
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status = 'AGUARDANDO_DEPENDENCIAS'
        ORDER BY criado_em ASC
    """)
    Flux<JobR2dbc> findJobsAguardandoDependencias();

    /**
     * Busca dependências de um job
     */
    @Query("""
        SELECT d.* FROM jobs j
        JOIN job_dependencias jd ON j.id = jd.job_id
        JOIN jobs d ON jd.dependencia_id = d.id
        WHERE j.id = :jobId
    """)
    Flux<JobR2dbc> findDependenciasDoJob(String jobId);

    /**
     * Busca jobs que dependem de outro job
     */
    @Query("""
        SELECT j.* FROM jobs j
        JOIN job_dependencias jd ON j.id = jd.job_id
        WHERE jd.dependencia_id = :dependenciaId
        AND j.status = 'AGUARDANDO_DEPENDENCIAS'
    """)
    Flux<JobR2dbc> findJobsDependentesDe(String dependenciaId);

    // === CONSULTAS DE AGENDAMENTO ===

    /**
     * Busca jobs agendados para execução
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status = 'AGENDADO'
        AND agendado_para <= :agora
        ORDER BY prioridade_peso DESC, agendado_para ASC
        LIMIT :limite
    """)
    Flux<JobR2dbc> findJobsAgendadosParaExecucao(LocalDateTime agora, int limite);

    /**
     * Busca próximos jobs agendados
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status = 'AGENDADO'
        AND agendado_para > :agora
        ORDER BY agendado_para ASC
        LIMIT :limite
    """)
    Flux<JobR2dbc> findProximosJobsAgendados(LocalDateTime agora, int limite);

    // === CONSULTAS DE MONITORAMENTO ===

    /**
     * Conta jobs por status
     */
    @Query("SELECT COUNT(*) FROM jobs WHERE status = :status")
    Mono<Long> countByStatus(StatusJob status);

    /**
     * Conta jobs ativos (não finalizados)
     */
    @Query("""
        SELECT COUNT(*) FROM jobs 
        WHERE status NOT IN ('COMPLETADO', 'CANCELADO', 'DESATIVADO', 'ARQUIVADO')
    """)
    Mono<Long> countJobsAtivos();

    /**
     * Busca jobs com muitas falhas
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE total_falhas >= :limitefalhas
        AND status NOT IN ('COMPLETADO', 'CANCELADO', 'DESATIVADO')
        ORDER BY total_falhas DESC, ultima_execucao_em DESC
        LIMIT :limite
    """)
    Flux<JobR2dbc> findJobsComMuitasFalhas(int limitefalhas, int limite);

    /**
     * Busca jobs com execução longa
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status IN ('EXECUTANDO', 'REEXECUTANDO', 'PROCESSANDO')
        AND iniciado_em < :tempoLimite
        ORDER BY iniciado_em ASC
    """)
    Flux<JobR2dbc> findJobsComExecucaoLonga(LocalDateTime tempoLimite);

    // === ESTATÍSTICAS E MÉTRICAS ===

    /**
     * Estatísticas gerais de jobs
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN status = 'COMPLETADO' THEN 1 ELSE 0 END) as concluidos,
            SUM(CASE WHEN status IN ('FALHADO', 'TIMEOUT') THEN 1 ELSE 0 END) as falhados,
            SUM(CASE WHEN status IN ('EXECUTANDO', 'REEXECUTANDO', 'PROCESSANDO') THEN 1 ELSE 0 END) as executando,
            AVG(CASE WHEN duracao_execucao_ms > 0 THEN duracao_execucao_ms ELSE NULL END) as duracao_media_ms
        FROM jobs 
        WHERE criado_em >= :dataInicio
    """)
    Mono<Object> getEstatisticasGerais(LocalDateTime dataInicio);

    /**
     * Estatísticas por tipo de job
     */
    @Query("""
        SELECT 
            tipo,
            COUNT(*) as total,
            SUM(CASE WHEN status = 'COMPLETADO' THEN 1 ELSE 0 END) as concluidos,
            SUM(CASE WHEN status IN ('FALHADO', 'TIMEOUT') THEN 1 ELSE 0 END) as falhados,
            AVG(CASE WHEN duracao_execucao_ms > 0 THEN duracao_execucao_ms ELSE NULL END) as duracao_media_ms
        FROM jobs 
        WHERE criado_em >= :dataInicio
        GROUP BY tipo
    """)
    Flux<Object> getEstatisticasPorTipo(LocalDateTime dataInicio);

    /**
     * Jobs mais demorados
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE status = 'COMPLETADO'
        AND duracao_execucao_ms > 0
        ORDER BY duracao_execucao_ms DESC
        LIMIT :limite
    """)
    Flux<JobR2dbc> findJobsMaisDemorados(int limite);

    // === LIMPEZA E MANUTENÇÃO ===

    /**
     * Remove jobs antigos completados
     */
    @Query("DELETE FROM jobs WHERE status = 'COMPLETADO' AND completado_em < :dataLimite")
    Mono<Integer> deleteJobsAntigosCompletados(LocalDateTime dataLimite);

    /**
     * Arquiva jobs antigos
     */
    @Query("""
        UPDATE jobs 
        SET status = 'ARQUIVADO', atualizado_em = :agora 
        WHERE status IN ('COMPLETADO', 'CANCELADO') 
        AND completado_em < :dataLimite
    """)
    Mono<Integer> arquivarJobsAntigos(LocalDateTime dataLimite, LocalDateTime agora);

    // === BUSCA PERSONALIZADA ===

    /**
     * Busca jobs por critérios customizados
     */
    @Query("""
        SELECT * FROM jobs 
        WHERE (:nome IS NULL OR nome LIKE CONCAT('%', :nome, '%'))
        AND (:grupo IS NULL OR grupo = :grupo)
        AND (:tipo IS NULL OR tipo = :tipo)
        AND (:status IS NULL OR status = :status)
        AND (:prioridade IS NULL OR prioridade = :prioridade)
        AND criado_em >= :dataInicio
        ORDER BY 
            CASE WHEN :ordenacao = 'prioridade' THEN prioridade_peso END DESC,
            CASE WHEN :ordenacao = 'data' THEN criado_em END DESC,
            CASE WHEN :ordenacao = 'duracao' THEN duracao_execucao_ms END DESC
        LIMIT :limite OFFSET :offset
    """)
    Flux<JobR2dbc> findJobsComFiltros(
        String nome, String grupo, String tipo, String status, String prioridade,
        LocalDateTime dataInicio, String ordenacao, int limite, int offset);

    /**
     * Conta jobs com filtros
     */
    @Query("""
        SELECT COUNT(*) FROM jobs 
        WHERE (:nome IS NULL OR nome LIKE CONCAT('%', :nome, '%'))
        AND (:grupo IS NULL OR grupo = :grupo)
        AND (:tipo IS NULL OR tipo = :tipo)
        AND (:status IS NULL OR status = :status)
        AND (:prioridade IS NULL OR prioridade = :prioridade)
        AND criado_em >= :dataInicio
    """)
    Mono<Long> countJobsComFiltros(
        String nome, String grupo, String tipo, String status, String prioridade,
        LocalDateTime dataInicio);
}