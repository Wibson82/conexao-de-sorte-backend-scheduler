package br.tec.facilitaservicos.scheduler.aplicacao.excecao;

/**
 * ============================================================================
 * 🚨 EXCEÇÃO PARA CIRCUIT BREAKER ABERTO
 * ============================================================================
 * 
 * Exceção lançada quando um job tem o circuit breaker aberto.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public class JobCircuitBreakerAbertoException extends RuntimeException {
    
    public JobCircuitBreakerAbertoException(String message) {
        super(message);
    }
    
    public JobCircuitBreakerAbertoException(String message, Throwable cause) {
        super(message, cause);
    }
}