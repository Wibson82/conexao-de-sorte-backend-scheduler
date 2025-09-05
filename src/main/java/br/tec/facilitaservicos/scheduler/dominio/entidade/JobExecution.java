package br.tec.facilitaservicos.scheduler.dominio.entidade;

import java.time.LocalDateTime;

/**
 * Entidade representando a execução de um job ETL.
 */
public class JobExecution {
    private final String jobId;
    private final String modalidade;
    private final String data;
    private final String chaveIdempotencia;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private JobStatus status;
    private String errorMessage;

    public JobExecution(String jobId, String modalidade, String data, 
                       String chaveIdempotencia, LocalDateTime startTime, JobStatus status) {
        this.jobId = jobId;
        this.modalidade = modalidade;
        this.data = data;
        this.chaveIdempotencia = chaveIdempotencia;
        this.startTime = startTime;
        this.status = status;
    }

    // Getters
    public String getJobId() {
        return jobId;
    }

    public String getModalidade() {
        return modalidade;
    }

    public String getData() {
        return data;
    }

    public String getChaveIdempotencia() {
        return chaveIdempotencia;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public JobStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    // Setters
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}