package br.tec.facilitaservicos.scheduler.aplicacao.excecao;

/**
 * ============================================================================
 * ❌ EXCEÇÃO PARA JOB NÃO ENCONTRADO
 * ============================================================================
 * 
 * Exceção lançada quando um job não é encontrado.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
public class JobNaoEncontradoException extends RuntimeException {
    
    public JobNaoEncontradoException(String message) {
        super(message);
    }
    
    public JobNaoEncontradoException(String message, Throwable cause) {
        super(message, cause);
    }
}