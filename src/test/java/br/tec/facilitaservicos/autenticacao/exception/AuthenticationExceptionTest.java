package br.tec.facilitaservicos.autenticacao.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes da AuthenticationException")
class AuthenticationExceptionTest {

    @Test
    @DisplayName("Deve criar exceção com mensagem")
    void testExceptionWithMessage() {
        String message = "Credenciais inválidas";
        AuthenticationException exception = new AuthenticationException(message);
        
        assertThat(exception.getMessage()).isEqualTo(message);
    }

    @Test
    @DisplayName("Deve criar exceção com mensagem e causa")
    void testExceptionWithMessageAndCause() {
        String message = "Erro de autenticação";
        RuntimeException cause = new RuntimeException("Erro interno");
        AuthenticationException exception = new AuthenticationException(message, cause);
        
        assertThat(exception.getMessage()).isEqualTo(message);
        assertThat(exception.getCause()).isEqualTo(cause);
    }
}
