package br.tec.facilitaservicos.scheduler.aplicacao.servico;

import br.tec.facilitaservicos.scheduler.dominio.entidade.JobR2dbc;
import br.tec.facilitaservicos.scheduler.dominio.repositorio.JobRepository;
import br.tec.facilitaservicos.scheduler.dominio.enums.StatusJob;
import br.tec.facilitaservicos.scheduler.dominio.enums.TipoJob;
import br.tec.facilitaservicos.scheduler.dominio.enums.PrioridadeJob;
import br.tec.facilitaservicos.scheduler.aplicacao.dto.JobDto;
import br.tec.facilitaservicos.scheduler.aplicacao.dto.EstatisticasJobDto;
import br.tec.facilitaservicos.scheduler.aplicacao.dto.CriarJobRequest;
import br.tec.facilitaservicos.scheduler.aplicacao.dto.AtualizarJobRequest;
import br.tec.facilitaservicos.scheduler.aplicacao.excecao.JobNaoEncontradoException;
import br.tec.facilitaservicos.scheduler.aplicacao.excecao.JobNaoPodeSerExecutadoException;
import br.tec.facilitaservicos.scheduler.aplicacao.excecao.JobCircuitBreakerAbertoException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.quartz.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * ⚙️ SERVIÇO DE GERENCIAMENTO DE JOBS
 * ============================================================================
 * 
 * Serviço principal para operações com jobs do scheduler.
 * Integra com Quartz Scheduler para execução distribuída.
 * 
 * 🚀 FUNCIONALIDADES PRINCIPAIS:
 * - CRUD completo de jobs
 * - Agendamento e execução
 * - Monitoramento e métricas
 * - Circuit breaker e retry
 * - Dependências entre jobs
 * - Pool de recursos
 * 
 * 🔄 CICLO DE VIDA:
 * - Criação → Agendamento → Execução → Conclusão
 * - Tratamento de falhas e retry automático
 * - Circuit breaker para proteção do sistema
 * - Cleanup automático de jobs antigos
 * 
 * 📊 OBSERVABILIDADE:
 * - Métricas detalhadas com Micrometer
 * - Logs estruturados
 * - Health checks
 * - Alertas automáticos
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobRepository jobRepository;
    private final Scheduler quartzScheduler;
    private final MeterRegistry meterRegistry;

    public JobService(JobRepository jobRepository, Scheduler quartzScheduler, MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.quartzScheduler = quartzScheduler;
        this.meterRegistry = meterRegistry;
    }
    
    // Cache para jobs em execução
    private final Map<String, LocalDateTime> jobsEmExecucao = new ConcurrentHashMap<>();
    
    // Métricas
    private final Counter jobsCriados = Counter.builder("jobs.criados")
        .description("Total de jobs criados")
        .register(meterRegistry);
    
    private final Counter jobsExecutados = Counter.builder("jobs.executados")
        .description("Total de jobs executados")
        .register(meterRegistry);
    
    private final Counter jobsFalhados = Counter.builder("jobs.falhados")
        .description("Total de jobs que falharam")
        .register(meterRegistry);
    
    private final Timer tempoExecucaoJobs = Timer.builder("jobs.tempo.execucao")
        .description("Tempo de execução dos jobs")
        .register(meterRegistry);

    // Gauge para jobs ativos
    {
        Gauge.builder("jobs.ativos")
            .description("Número de jobs ativos")
            .register(meterRegistry, this, JobService::contarJobsAtivos);
    }

    // === OPERAÇÕES CRUD ===

    /**
     * Cria um novo job
     */
    @Transactional
    public Mono<JobDto> criarJob(CriarJobRequest request) {
        log.info("🆕 Criando novo job: {}", request.getNome());
        
        return Mono.fromCallable(() -> JobR2dbc.builder()
                .nome(request.getNome())
                .descricao(request.getDescricao())
                .tipo(request.getTipo())
                .prioridade(request.getPrioridade())
                .grupo(request.getGrupo())
                .cronExpression(request.getCronExpression())
                .parametros(request.getParametros())
                .timeoutSegundos(request.getTimeoutSegundos())
                .maxTentativas(request.getMaxTentativas())
                .permitirExecucaoConcorrente(Boolean.FALSE)
                .build())
            .flatMap(jobRepository::save)
            .doOnSuccess(job -> {
                jobsCriados.increment();
                log.info("✅ Job criado com sucesso: {} (ID: {})", job.getNome(), job.getId());
            })
            .map(this::toDto)
            .doOnError(error -> log.error("❌ Erro ao criar job: {}", error.getMessage(), error));
    }

    /**
     * Busca job por ID
     */
    public Mono<JobDto> buscarJobPorId(String id) {
        return jobRepository.findById(id)
            .map(this::toDto)
            .switchIfEmpty(Mono.error(new JobNaoEncontradoException("Job não encontrado: " + id)));
    }

    /**
     * Lista jobs com filtros
     */
    public Flux<JobDto> listarJobs(String nome, String grupo, TipoJob tipo, 
                                   StatusJob status, PrioridadeJob prioridade,
                                   LocalDateTime dataInicio, String ordenacao, 
                                   int limite, int offset) {
        
        return jobRepository.findJobsComFiltros(
                nome, grupo, 
                tipo != null ? tipo.name() : null,
                status != null ? status.name() : null,
                prioridade != null ? prioridade.name() : null,
                dataInicio != null ? dataInicio : LocalDateTime.now().minusDays(30),
                ordenacao != null ? ordenacao : "data",
                limite, offset
            )
            .map(this::toDto)
            .doOnSubscribe(s -> log.debug("🔍 Buscando jobs com filtros: nome={}, grupo={}, tipo={}", 
                nome, grupo, tipo));
    }

    /**
     * Conta jobs com filtros
     */
    public Mono<Long> contarJobs(String nome, String grupo, TipoJob tipo, 
                                StatusJob status, PrioridadeJob prioridade,
                                LocalDateTime dataInicio) {
        
        return jobRepository.countJobsComFiltros(
            nome, grupo,
            tipo != null ? tipo.name() : null,
            status != null ? status.name() : null,
            prioridade != null ? prioridade.name() : null,
            dataInicio != null ? dataInicio : LocalDateTime.now().minusDays(30)
        );
    }

    /**
     * Atualiza um job
     */
    @Transactional
    public Mono<JobDto> atualizarJob(String id, AtualizarJobRequest request) {
        return jobRepository.findById(id)
            .switchIfEmpty(Mono.error(new JobNaoEncontradoException("Job não encontrado: " + id)))
            .flatMap(job -> {
                // Verificar se job pode ser editado
                if (!job.getStatus().podeEditar()) {
                    return Mono.error(new JobNaoPodeSerExecutadoException(
                        "Job não pode ser editado no status: " + job.getStatus()));
                }
                
                // Atualizar campos
                if (request.getNome() != null) job.setNome(request.getNome());
                if (request.getDescricao() != null) job.setDescricao(request.getDescricao());
                if (request.getPrioridade() != null) job.setPrioridade(request.getPrioridade());
                if (request.getCronExpression() != null) job.setCronExpression(request.getCronExpression());
                if (request.getParametros() != null) job.setParametros(request.getParametros());
                if (request.getTimeoutSegundos() != null) job.setTimeoutSegundos(request.getTimeoutSegundos());
                if (request.getMaxTentativas() != null) job.setMaxTentativas(request.getMaxTentativas());
                if (request.getAtivo() != null) job.setAtivo(request.getAtivo());
                
                job.setAtualizadoEm(LocalDateTime.now());
                
                return jobRepository.save(job);
            })
            .map(this::toDto)
            .doOnSuccess(job -> log.info("📝 Job atualizado: {} (ID: {})", job.getNome(), job.getId()));
    }

    /**
     * Remove um job
     */
    @Transactional
    public Mono<Void> removerJob(String id) {
        return jobRepository.findById(id)
            .switchIfEmpty(Mono.error(new JobNaoEncontradoException("Job não encontrado: " + id)))
            .flatMap(job -> {
                // Verificar se job pode ser removido
                if (!job.getStatus().podeRemover()) {
                    return Mono.error(new JobNaoPodeSerExecutadoException(
                        "Job não pode ser removido no status: " + job.getStatus()));
                }
                
                return jobRepository.delete(job)
                    .then(removerJobDoQuartz(job.getId()))
                    .doOnSuccess(v -> log.info("🗑️ Job removido: {} (ID: {})", job.getNome(), job.getId()));
            });
    }

    // === OPERAÇÕES DE AGENDAMENTO ===

    /**
     * Agenda job para execução
     */
    @Transactional
    public Mono<JobDto> agendarJob(String id) {
        return jobRepository.findById(id)
            .switchIfEmpty(Mono.error(new JobNaoEncontradoException("Job não encontrado: " + id)))
            .flatMap(job -> {
                // Verificar se job pode ser executado
                if (!job.getStatus().isPodeExecutar()) {
                    return Mono.error(new JobNaoPodeSerExecutadoException(
                        "Job não pode ser executado no status: " + job.getStatus()));
                }
                
                // Verificar circuit breaker
                if (job.getCircuitBreakerAberto()) {
                    return Mono.error(new JobCircuitBreakerAbertoException(
                        "Job com circuit breaker aberto: " + id));
                }
                
                // Agendar no Quartz
                return agendarNoQuartz(job)
                    .then(Mono.fromCallable(() -> {
                        job.setStatus(StatusJob.AGENDADO);
                        job.setAtualizadoEm(LocalDateTime.now());
                        return job;
                    }))
                    .flatMap(jobRepository::save);
            })
            .map(this::toDto)
            .doOnSuccess(job -> log.info("📅 Job agendado: {} (ID: {})", job.getNome(), job.getId()));
    }

    /**
     * Executa job imediatamente
     */
    @Transactional
    public Mono<JobDto> executarJob(String id) {
        return jobRepository.findById(id)
            .switchIfEmpty(Mono.error(new JobNaoEncontradoException("Job não encontrado: " + id)))
            .flatMap(job -> {
                // Verificar se job pode ser executado
                if (!job.getStatus().isPodeExecutar()) {
                    return Mono.error(new JobNaoPodeSerExecutadoException(
                        "Job não pode ser executado no status: " + job.getStatus()));
                }
                
                // Verificar circuit breaker
                if (job.getCircuitBreakerAberto()) {
                    return Mono.error(new JobCircuitBreakerAbertoException(
                        "Job com circuit breaker aberto: " + id));
                }
                
                // Executar no Quartz
                return executarNoQuartz(job)
                    .then(Mono.fromCallable(() -> {
                        job.setStatus(StatusJob.EXECUTANDO);
                        job.setIniciadoEm(LocalDateTime.now());
                        job.setExecucoesAtivas(job.getExecucoesAtivas() + 1);
                        job.setTentativas(job.getTentativas() + 1);
                        job.calcularTimeoutEm();
                        job.setAtualizadoEm(LocalDateTime.now());
                        return job;
                    }))
                    .flatMap(jobRepository::save);
            })
            .map(this::toDto)
            .doOnSuccess(job -> {
                jobsExecutados.increment();
                jobsEmExecucao.put(job.getId(), LocalDateTime.now());
                log.info("▶️ Job iniciado: {} (ID: {})", job.getNome(), job.getId());
            });
    }

    /**
     * Cancela job
     */
    @Transactional
    public Mono<JobDto> cancelarJob(String id) {
        return jobRepository.findById(id)
            .switchIfEmpty(Mono.error(new JobNaoEncontradoException("Job não encontrado: " + id)))
            .flatMap(job -> {
                // Verificar se job pode ser cancelado
                if (!job.getStatus().isPodeCancelar()) {
                    return Mono.error(new JobNaoPodeSerExecutadoException(
                        "Job não pode ser cancelado no status: " + job.getStatus()));
                }
                
                // Cancelar no Quartz
                return cancelarNoQuartz(job.getId())
                    .then(Mono.fromCallable(() -> {
                        job.setStatus(StatusJob.CANCELADO);
                        job.setCompletadoEm(LocalDateTime.now());
                        job.setAtualizadoEm(LocalDateTime.now());
                        return job;
                    }))
                    .flatMap(jobRepository::save);
            })
            .map(this::toDto)
            .doOnSuccess(job -> {
                jobsEmExecucao.remove(job.getId());
                log.info("⏹️ Job cancelado: {} (ID: {})", job.getNome(), job.getId());
            });
    }

    // === OPERAÇÕES DE MONITORAMENTO ===

    /**
     * Busca jobs prontos para execução
     */
    public Flux<JobDto> buscarJobsProntosParaExecucao(int limite) {
        return jobRepository.findJobsProntosParaExecucao(LocalDateTime.now(), limite)
            .map(this::toDto)
            .doOnSubscribe(s -> log.debug("🔍 Buscando jobs prontos para execução (limite: {})", limite));
    }

    /**
     * Busca jobs com timeout
     */
    public Flux<JobDto> buscarJobsComTimeout() {
        return jobRepository.findJobsComTimeout(LocalDateTime.now())
            .map(this::toDto)
            .doOnNext(job -> log.warn("⏱️ Job com timeout detectado: {} (ID: {})", job.getNome(), job.getId()));
    }

    /**
     * Busca jobs para retry
     */
    public Flux<JobDto> buscarJobsParaRetry(int limite) {
        return jobRepository.findJobsParaRetry(LocalDateTime.now(), limite)
            .map(this::toDto)
            .doOnNext(job -> log.info("🔄 Job para retry: {} (ID: {})", job.getNome(), job.getId()));
    }

    /**
     * Reset de circuit breaker
     */
    @Transactional
    public Flux<JobDto> resetCircuitBreaker() {
        LocalDateTime tempoLimite = LocalDateTime.now().minus(Duration.ofMinutes(30));
        
        return jobRepository.findJobsCircuitBreakerParaReset(tempoLimite)
            .flatMap(job -> {
                job.resetCircuitBreaker();
                job.setAtualizadoEm(LocalDateTime.now());
                return jobRepository.save(job);
            })
            .map(this::toDto)
            .doOnNext(job -> log.info("🔧 Circuit breaker resetado: {} (ID: {})", job.getNome(), job.getId()));
    }

    // === ESTATÍSTICAS E MÉTRICAS ===

    /**
     * Obtém estatísticas gerais
     */
    public Mono<EstatisticasJobDto> obterEstatisticasGerais(LocalDateTime dataInicio) {
        return jobRepository.getEstatisticasGerais(dataInicio)
            .cast(Map.class)
            .map(stats -> EstatisticasJobDto.builder()
                .total(((Number) stats.get("total")).longValue())
                .concluidos(((Number) stats.get("concluidos")).longValue())
                .falhados(((Number) stats.get("falhados")).longValue())
                .executando(((Number) stats.get("executando")).longValue())
                .duracaoMediaMs(((Number) stats.get("duracao_media_ms")).doubleValue())
                .build());
    }

    /**
     * Obtém estatísticas por tipo
     */
    public Flux<Map<String, Object>> obterEstatisticasPorTipo(LocalDateTime dataInicio) {
        return jobRepository.getEstatisticasPorTipo(dataInicio)
            .cast(Map.class);
    }

    // === OPERAÇÕES DE LIMPEZA ===

    /**
     * Remove jobs antigos completados
     */
    @Transactional
    public Mono<Integer> removerJobsAntigosCompletados(Duration idade) {
        LocalDateTime dataLimite = LocalDateTime.now().minus(idade);
        return jobRepository.deleteJobsAntigosCompletados(dataLimite)
            .doOnSuccess(count -> log.info("🧹 {} jobs antigos removidos", count));
    }

    /**
     * Arquiva jobs antigos
     */
    @Transactional
    public Mono<Integer> arquivarJobsAntigos(Duration idade) {
        LocalDateTime dataLimite = LocalDateTime.now().minus(idade);
        LocalDateTime agora = LocalDateTime.now();
        return jobRepository.arquivarJobsAntigos(dataLimite, agora)
            .doOnSuccess(count -> log.info("📦 {} jobs arquivados", count));
    }

    // === MÉTODOS PRIVADOS - INTEGRAÇÃO QUARTZ ===

    private Mono<Void> agendarNoQuartz(JobR2dbc job) {
        return Mono.fromCallable(() -> {
                try {
                    JobDetail jobDetail = JobBuilder.newJob(ExecutorJob.class)
                        .withIdentity(job.getId(), job.getGrupo())
                        .usingJobData("jobId", job.getId())
                        .build();
                    
                    Trigger trigger;
                    if (job.getCronExpression() != null && !job.getCronExpression().isEmpty()) {
                        trigger = TriggerBuilder.newTrigger()
                            .withIdentity(job.getId() + "_trigger", job.getGrupo())
                            .withSchedule(CronScheduleBuilder.cronSchedule(job.getCronExpression()))
                            .build();
                    } else {
                        trigger = TriggerBuilder.newTrigger()
                            .withIdentity(job.getId() + "_trigger", job.getGrupo())
                            .startAt(java.util.Date.from(job.getAgendadoPara()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant()))
                            .build();
                    }
                    
                    quartzScheduler.scheduleJob(jobDetail, trigger);
                    return null;
                } catch (SchedulerException e) {
                    throw new RuntimeException("Erro ao agendar job no Quartz", e);
                }
            })
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }

    private Mono<Void> executarNoQuartz(JobR2dbc job) {
        return Mono.fromCallable(() -> {
                try {
                    JobKey jobKey = new JobKey(job.getId(), job.getGrupo());
                    if (quartzScheduler.checkExists(jobKey)) {
                        quartzScheduler.triggerJob(jobKey);
                    } else {
                        // Criar job temporário se não existe
                        JobDetail jobDetail = JobBuilder.newJob(ExecutorJob.class)
                            .withIdentity(job.getId(), job.getGrupo())
                            .usingJobData("jobId", job.getId())
                            .storeDurably(false)
                            .build();
                        
                        quartzScheduler.addJob(jobDetail, false);
                        quartzScheduler.triggerJob(jobKey);
                    }
                    return null;
                } catch (SchedulerException e) {
                    throw new RuntimeException("Erro ao executar job no Quartz", e);
                }
            })
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }

    private Mono<Void> cancelarNoQuartz(String jobId) {
        return Mono.fromCallable(() -> {
                try {
                    quartzScheduler.interrupt(new JobKey(jobId));
                    return null;
                } catch (SchedulerException e) {
                    throw new RuntimeException("Erro ao cancelar job no Quartz", e);
                }
            })
            .onErrorResume(e -> {
                log.warn("Não foi possível cancelar job no Quartz: {}", e.getMessage());
                return Mono.empty();
            });
    }

    private Mono<Void> removerJobDoQuartz(String jobId) {
        return Mono.fromCallable(() -> {
                try {
                    quartzScheduler.deleteJob(new JobKey(jobId));
                    return null;
                } catch (SchedulerException e) {
                    throw new RuntimeException("Erro ao remover job do Quartz", e);
                }
            })
            .onErrorResume(e -> {
                log.warn("Não foi possível remover job do Quartz: {}", e.getMessage());
                return Mono.empty();
            });
    }

    // === MÉTODOS UTILITÁRIOS ===

    private JobDto toDto(JobR2dbc job) {
        JobDto dto = new JobDto();
        dto.setId(job.getId());
        dto.setNome(job.getNome());
        dto.setDescricao(job.getDescricao());
        dto.setTipo(job.getTipo());
        dto.setStatus(job.getStatus());
        dto.setPrioridade(job.getPrioridade());
        dto.setGrupo(job.getGrupo());
        dto.setCronExpression(job.getCronExpression());
        dto.setParametros(job.getParametros());
        dto.setTimeoutSegundos(job.getTimeoutSegundos());
        dto.setMaxTentativas(job.getMaxTentativas());
        dto.setTentativas(job.getTentativas());
        dto.setMaxExecucoesConcorrentes(job.getMaxExecucoesConcorrentes());
        dto.setExecucoesAtivas(job.getExecucoesAtivas());
        dto.setTotalExecucoes(job.getTotalExecucoes());
        dto.setTotalSucessos(job.getTotalSucessos());
        dto.setTotalFalhas(job.getTotalFalhas());
        dto.setUltimoErro(job.getUltimoErro());
        dto.setDuracaoExecucaoMs(job.getDuracaoExecucaoMs());
        dto.setCircuitBreakerAberto(job.getCircuitBreakerAberto());
        dto.setCircuitBreakerFailures(job.getCircuitBreakerFailures());
        dto.setAgendadoPara(job.getAgendadoPara());
        dto.setIniciadoEm(job.getIniciadoEm());
        dto.setCompletadoEm(job.getCompletadoEm());
        dto.setProximoRetry(job.getProximoRetry());
        dto.setTimeoutEm(job.getTimeoutEm());
        dto.setAtivo(job.getAtivo());
        dto.setCriadoEm(job.getCriadoEm());
        dto.setAtualizadoEm(job.getAtualizadoEm());
        return dto;
    }

    private double contarJobsAtivos() {
        return jobRepository.countJobsAtivos().block(Duration.ofSeconds(5));
    }

    // Classe interna para execução de jobs
    public static class ExecutorJob implements Job {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            String jobId = context.getJobDetail().getJobDataMap().getString("jobId");
            // Implementar lógica de execução específica
            // Pode ser injetado via Spring
        }
    }
}
