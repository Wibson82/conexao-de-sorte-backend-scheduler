package br.tec.facilitaservicos.scheduler.aplicacao.job;

import br.tec.facilitaservicos.scheduler.aplicacao.servico.etl.ServicoExtracaoLoteria;
import br.tec.facilitaservicos.scheduler.configuracao.SchedulerProperties;
import br.tec.facilitaservicos.scheduler.dominio.entidade.JobExecution;
import br.tec.facilitaservicos.scheduler.dominio.entidade.JobStatus;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Job de ETL para loterias.
 * Extrai → processa → persiste com idempotência e retries.
 */
@Component
public class LoteriasETLJob {

    private static final Logger logger = LoggerFactory.getLogger(LoteriasETLJob.class);
    
    private final SchedulerProperties properties;
    private final ServicoExtracaoLoteria servicoExtracao;
    private final Map<String, JobExecution> jobExecutions = new ConcurrentHashMap<>();
    private final Counter successCounter;
    private final Counter errorCounter;
    private final Counter circuitBreakerOpenCounter;
    private final Timer executionTimer;
    private final CircuitBreaker circuitBreaker;
    private final MeterRegistry meterRegistry;

    public LoteriasETLJob(SchedulerProperties properties, 
                         ServicoExtracaoLoteria servicoExtracao,
                         MeterRegistry meterRegistry,
                         CircuitBreakerRegistry circuitBreakerRegistry) {
        this.properties = properties;
        this.servicoExtracao = servicoExtracao;
        this.meterRegistry = meterRegistry;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("scheduler-etl");
        
        this.successCounter = Counter.builder("scheduler.etl.jobs.success")
                .description("Number of successful ETL jobs")
                .register(meterRegistry);
        this.errorCounter = Counter.builder("scheduler.etl.jobs.error")
                .description("Number of failed ETL jobs")
                .register(meterRegistry);
        this.circuitBreakerOpenCounter = Counter.builder("scheduler.etl.jobs.circuit_breaker_open")
                .description("Number of times circuit breaker opened")
                .register(meterRegistry);
        this.executionTimer = Timer.builder("scheduler.etl.jobs.execution.time")
                .description("Time taken to execute ETL jobs")
                .register(meterRegistry);
    }

    /**
     * Executa job de ETL com idempotência, retries e circuit breaker.
     */
    public Mono<JobExecution> executar(String jobId, String modalidade, String data) {
        return Mono.fromCallable(() -> {
            // Configurar MDC para logs estruturados
            MDC.put("jobId", jobId);
            MDC.put("modalidade", modalidade);
            MDC.put("data", data != null ? data : "hoje");
            
            if (!properties.etl().loterias().enabled()) {
                logger.warn(Markers.append("etl.disabled", true), 
                           "ETL de loterias está desabilitado");
                throw new IllegalStateException("ETL de loterias desabilitado");
            }
            
            return jobId;
        })
        .flatMap(id -> executarComIdempotencia(id, modalidade, data))
        .doFinally(signalType -> {
            // Limpar MDC
            MDC.clear();
        });
    }
    
    private Mono<JobExecution> executarComIdempotencia(String jobId, String modalidade, String data) {
        // Verificar idempotência
        String chaveIdempotencia = gerarChaveIdempotencia(modalidade, data);
        
        if (jobExecutions.containsKey(chaveIdempotencia)) {
            JobExecution existente = jobExecutions.get(chaveIdempotencia);
            logger.info(Markers.append("idempotencia", true)
                              .and(Markers.append("chave", chaveIdempotencia)), 
                       "Job já executado (idempotência)");
            return Mono.just(existente);
        }

        JobExecution execution = new JobExecution(
            jobId,
            modalidade,
            data,
            chaveIdempotencia,
            LocalDateTime.now(),
            JobStatus.RUNNING
        );
        
        jobExecutions.put(chaveIdempotencia, execution);
        
        logger.info(Markers.append("chaveIdempotencia", chaveIdempotencia)
                          .and(Markers.append("startTime", execution.getStartTime())), 
                   "Iniciando execução do job ETL");
        
        Timer.Sample sample = Timer.start(meterRegistry);
        return executarComRetryECircuitBreaker(execution)
                .doFinally(signalType -> sample.stop(executionTimer))
                .doOnNext(result -> {
                    successCounter.increment();
                    logger.info(Markers.append("executionTime", 
                                              Duration.between(execution.getStartTime(), 
                                                             execution.getEndTime() != null ? execution.getEndTime() : LocalDateTime.now())), 
                               "Job ETL executado com sucesso");
                })
                .doOnError(error -> {
                    errorCounter.increment();
                    execution.setStatus(JobStatus.FAILED);
                    execution.setEndTime(LocalDateTime.now());
                    execution.setErrorMessage(error.getMessage());
                    
                    if (error instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                        circuitBreakerOpenCounter.increment();
                        logger.error(Markers.append("circuitBreakerOpen", true), 
                                    "Falha no job ETL - Circuit Breaker Aberto", error);
                    } else {
                        logger.error(Markers.append("errorType", error.getClass().getSimpleName()), 
                                    "Falha no job ETL", error);
                    }
                });
    }

    private Mono<JobExecution> executarComRetryECircuitBreaker(JobExecution execution) {
        return executarETL(execution)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .retryWhen(Retry.backoff(
                    properties.etl().loterias().maxRetries(),
                    Duration.ofMillis(properties.etl().loterias().backoffMs())
                ).filter(this::isRetryable)
                .doBeforeRetry(retrySignal -> {
                    logger.warn(Markers.append("retryAttempt", retrySignal.totalRetries() + 1)
                                      .and(Markers.append("maxRetries", properties.etl().loterias().maxRetries())), 
                               "Tentativa de retry do job ETL", retrySignal.failure());
                }));
    }

    private Mono<JobExecution> executarETL(JobExecution execution) {
        logger.debug("Executando ETL: jobId={}, modalidade={}, data={}", 
                    execution.getJobId(), execution.getModalidade(), execution.getData());
        
        // Integração com ServicoExtracaoLoteria
        return servicoExtracao.extrairResultados(execution.getModalidade(), execution.getData())
                .map(resultados -> {
                    logger.info("ETL executado com sucesso: jobId={}, resultados extraídos={}", 
                               execution.getJobId(), resultados.size());
                    execution.setStatus(JobStatus.COMPLETED);
                    execution.setEndTime(LocalDateTime.now());
                    return execution;
                })
        .doOnError(error -> {
            execution.setStatus(JobStatus.FAILED);
            execution.setEndTime(LocalDateTime.now());
            execution.setErrorMessage(error.getMessage());
        });
    }

    private boolean isRetryable(Throwable throwable) {
        // Definir quais exceções são "retryable"
        return !(throwable instanceof IllegalArgumentException);
    }

    private String gerarChaveIdempotencia(String modalidade, String data) {
        if (data != null) {
            return String.format("%s:%s", modalidade, data);
        }
        return String.format("%s:hoje", modalidade);
    }

    public Map<String, JobExecution> getJobExecutions() {
        return Map.copyOf(jobExecutions);
    }
}