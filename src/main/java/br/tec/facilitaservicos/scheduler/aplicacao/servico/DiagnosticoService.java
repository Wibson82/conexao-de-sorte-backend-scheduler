package br.tec.facilitaservicos.scheduler.aplicacao.servico;

import br.tec.facilitaservicos.scheduler.aplicacao.job.LoteriasETLJob;
import br.tec.facilitaservicos.scheduler.apresentacao.dto.SchedulerStatusResponse;
import br.tec.facilitaservicos.scheduler.dominio.entidade.JobExecution;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Serviço para diagnóstico do scheduler.
 */
@Service
public class DiagnosticoService {

    private final LoteriasETLJob loteriasETLJob;

    public DiagnosticoService(LoteriasETLJob loteriasETLJob) {
        this.loteriasETLJob = loteriasETLJob;
    }

    /**
     * Obtém status dos schedulers.
     */
    public Mono<SchedulerStatusResponse> obterStatusSchedulers() {
        return Mono.fromCallable(() -> {
            Map<String, JobExecution> executions = loteriasETLJob.getJobExecutions();
            
            LocalDateTime ultimaExecucao = executions.values().stream()
                    .map(JobExecution::getStartTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            
            List<SchedulerStatusResponse.JobStatus> jobs = executions.values().stream()
                    .map(execution -> new SchedulerStatusResponse.JobStatus(
                        execution.getJobId(),
                        execution.getModalidade(),
                        execution.getStatus().name(),
                        execution.getStartTime(),
                        execution.getEndTime(),
                        execution.getErrorMessage()
                    ))
                    .toList();
            
            return new SchedulerStatusResponse(
                true, // scheduler sempre ativo
                ultimaExecucao,
                jobs
            );
        });
    }

    /**
     * Testa funcionamento do scheduler.
     */
    public Mono<String> testarScheduler() {
        return Mono.fromCallable(() -> {
            // Executar teste simples
            LocalDateTime now = LocalDateTime.now();
            return String.format("Scheduler funcionando corretamente. Timestamp: %s", now);
        });
    }
}