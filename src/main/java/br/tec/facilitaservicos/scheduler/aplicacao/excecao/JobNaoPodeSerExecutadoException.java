package br.tec.facilitaservicos.scheduler.aplicacao.excecao;

/**
 * ============================================================================
 * ⚠️ EXCEÇÃO PARA JOB QUE NÃO PODE SER EXECUTADO
 * ============================================================================
 * 
 * Exceção lançada quando um job não pode ser executado devido ao seu status.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public class JobNaoPodeSerExecutadoException extends RuntimeException {
    
    public JobNaoPodeSerExecutadoException(String message) {
        super(message);
    }
    
    public JobNaoPodeSerExecutadoException(String message, Throwable cause) {
        super(message, cause);
    }
}