package br.tec.facilitaservicos.scheduler.apresentacao.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response com status dos schedulers.
 */
public record SchedulerStatusResponse(
    boolean active,
    LocalDateTime lastExecution,
    List<JobStatus> jobs
) {
    public record JobStatus(
        String jobId,
        String modalidade,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String errorMessage
    ) {}
}