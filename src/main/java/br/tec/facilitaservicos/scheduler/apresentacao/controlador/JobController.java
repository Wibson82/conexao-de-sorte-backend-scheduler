package br.tec.facilitaservicos.scheduler.apresentacao.controlador;

import br.tec.facilitaservicos.scheduler.aplicacao.job.LoteriasETLJob;
import br.tec.facilitaservicos.scheduler.aplicacao.servico.DiagnosticoService;
import br.tec.facilitaservicos.scheduler.apresentacao.dto.EtlJobRequest;
import br.tec.facilitaservicos.scheduler.apresentacao.dto.EtlJobResponse;
import br.tec.facilitaservicos.scheduler.apresentacao.dto.SchedulerStatusResponse;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Controlador para gerenciamento de jobs ETL.
 */
@RestController
@RequestMapping("/jobs")
public class JobController {

    private static final Logger logger = LoggerFactory.getLogger(JobController.class);
    
    private final LoteriasETLJob loteriasETLJob;
    private final DiagnosticoService diagnosticoService;
    private final Counter jobsCreatedCounter;

    public JobController(LoteriasETLJob loteriasETLJob, 
                        DiagnosticoService diagnosticoService,
                        MeterRegistry meterRegistry) {
        this.loteriasETLJob = loteriasETLJob;
        this.diagnosticoService = diagnosticoService;
        this.jobsCreatedCounter = Counter.builder("scheduler.jobs.created")
                .description("Total number of ETL jobs created")
                .register(meterRegistry);
    }

    /**
     * Inicia job de ETL para loterias.
     * POST /jobs/loterias/etl body { modalidade: string, data?: yyyy-MM-dd } → { jobId: string }
     */
    @PostMapping("/loterias/etl")
    @PreAuthorize("hasAuthority('SCOPE_scheduler.write')")
    @Timed(value = "scheduler.jobs.creation.time", description = "Time taken to create ETL job")
    public Mono<ResponseEntity<EtlJobResponse>> iniciarETLLoterias(@Valid @RequestBody EtlJobRequest request) {
        String jobId = UUID.randomUUID().toString();
        
        logger.info("Iniciando job ETL: jobId={}, modalidade={}, data={}", 
                   jobId, request.modalidade(), request.data());
        
        return loteriasETLJob.executar(jobId, request.modalidade(), request.data())
                .doOnNext(result -> {
                    jobsCreatedCounter.increment();
                    logger.info("Job ETL iniciado com sucesso: jobId={}", jobId);
                })
                .map(result -> ResponseEntity.ok(new EtlJobResponse(jobId)))
                .doOnError(error -> logger.error("Erro ao iniciar job ETL: jobId={}", jobId, error))
                .onErrorReturn(ResponseEntity.internalServerError()
                               .body(new EtlJobResponse(jobId)));
    }

    /**
     * Obtém status dos schedulers.
     * GET /diagnostico/schedulers → status
     */
    @GetMapping("/diagnostico/schedulers")
    @PreAuthorize("hasAuthority('SCOPE_scheduler.read')")
    public Mono<ResponseEntity<SchedulerStatusResponse>> obterStatusSchedulers() {
        logger.debug("Consultando status dos schedulers");
        
        return diagnosticoService.obterStatusSchedulers()
                .map(ResponseEntity::ok)
                .doOnNext(response -> logger.debug("Status dos schedulers obtido com sucesso"))
                .doOnError(error -> logger.error("Erro ao obter status dos schedulers", error))
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    /**
     * Testa funcionamento do scheduler.
     * GET /diagnostico/teste-scheduler → ok
     */
    @GetMapping("/diagnostico/teste-scheduler")
    @PreAuthorize("hasAuthority('SCOPE_scheduler.read')")
    public Mono<ResponseEntity<String>> testarScheduler() {
        logger.debug("Executando teste do scheduler");
        
        return diagnosticoService.testarScheduler()
                .map(resultado -> ResponseEntity.ok(resultado))
                .doOnNext(response -> logger.debug("Teste do scheduler executado com sucesso"))
                .doOnError(error -> logger.error("Erro no teste do scheduler", error))
                .onErrorReturn(ResponseEntity.internalServerError().body("Erro no teste"));
    }
}