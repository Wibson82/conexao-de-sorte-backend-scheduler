package br.tec.facilitaservicos.autenticacao.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes do RequisicaoLoginDTO")
class RequisicaoLoginDTOTest {

    @Test
    @DisplayName("Deve criar RequisicaoLoginDTO válida")
    void testValidLoginRequest() {
        RequisicaoLoginDTO request = new RequisicaoLoginDTO("user@test.com", "password123");
        
        assertThat(request.usuario()).isEqualTo("user@test.com");
        assertThat(request.senha()).isEqualTo("password123");
    }

    @Test
    @DisplayName("Deve trim dos valores de entrada")
    void testTrimValues() {
        RequisicaoLoginDTO request = new RequisicaoLoginDTO("  user@test.com  ", "  password123  ");
        
        assertThat(request.usuario()).isEqualTo("user@test.com");
        assertThat(request.senha()).isEqualTo("password123");
    }

    @Test
    @DisplayName("Deve gerar string segura para logs")
    void testSecureString() {
        RequisicaoLoginDTO request = new RequisicaoLoginDTO("user@test.com", "password123");
        
        String secureString = request.toSecureString();
        
        assertThat(secureString).contains("RequisicaoLoginDTO");
        assertThat(secureString).contains("user@test.com");
        assertThat(secureString).contains("[PROTEGIDA]");
        assertThat(secureString).doesNotContain("password123");
    }

    @Test
    @DisplayName("Deve usar toString seguro")
    void testToString() {
        RequisicaoLoginDTO request = new RequisicaoLoginDTO("user@test.com", "password123");
        
        String toString = request.toString();
        
        assertThat(toString).contains("[PROTEGIDA]");
        assertThat(toString).doesNotContain("password123");
    }
}
