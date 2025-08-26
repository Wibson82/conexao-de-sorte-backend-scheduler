package br.tec.facilitaservicos.autenticacao.exception;

/**
 * Exceção para erros relacionados a tokens JWT.
 */
public class TokenException extends RuntimeException {
    
    public TokenException(String message) {
        super(message);
    }
    
    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }
}