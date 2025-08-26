package br.tec.facilitaservicos.autenticacao.exception;

/**
 * Exceção para erros relacionados ao Azure Key Vault.
 */
public class KeyVaultException extends RuntimeException {
    
    public KeyVaultException(String message) {
        super(message);
    }
    
    public KeyVaultException(String message, Throwable cause) {
        super(message, cause);
    }
}