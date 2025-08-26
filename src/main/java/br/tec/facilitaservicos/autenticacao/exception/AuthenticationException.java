package br.tec.facilitaservicos.autenticacao.exception;

/**
 * Exceção para erros de autenticação.
 */
public class AuthenticationException extends RuntimeException {
    
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}