package br.tec.facilitaservicos.autenticacao.dto;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes do RespostaTokenDTO")
class RespostaTokenDTOTest {

    @Test
    @DisplayName("Deve criar RespostaTokenDTO básica")
    void testBasicTokenResponse() {
        RespostaTokenDTO response = RespostaTokenDTO.of("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature", "refresh_token", 3600L);
        
        assertThat(response.tokenAcesso()).isEqualTo("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature");
        assertThat(response.tokenRenovacao()).isEqualTo("refresh_token");
        assertThat(response.tipoToken()).isEqualTo("Bearer");
        assertThat(response.tempoValidadeSegundos()).isEqualTo(3600L);
        assertThat(response.dataExpiracao()).isAfter(LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve criar RespostaTokenDTO com permissões")
    void testTokenResponseWithPermissions() {
        Set<String> permissions = Set.of("read", "write");
        RespostaTokenDTO response = RespostaTokenDTO.of("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature", "refresh_token", 3600L, permissions);
        
        assertThat(response.permissoes()).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    @DisplayName("Deve validar formato JWT do token de acesso")
    void testJwtFormatValidation() {
        assertThatThrownBy(() -> RespostaTokenDTO.of("invalid_token", "refresh_token", 3600L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Token de acesso não está em formato JWT válido");
    }

    @Test
    @DisplayName("Deve verificar se token está expirado")
    void testTokenExpiration() {
        RespostaTokenDTO expiredResponse = new RespostaTokenDTO(
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature",
            "refresh_token",
            "Bearer",
            3600L,
            LocalDateTime.now().minusHours(1),
            null,
            null,
            null
        );
        
        assertThat(expiredResponse.isExpirado()).isTrue();
    }

    @Test
    @DisplayName("Deve mascarar token para logs seguros")
    void testTokenMasking() {
        RespostaTokenDTO response = RespostaTokenDTO.of("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature", "refresh_token", 3600L);
        
        String maskedToken = response.getTokenAcessoMascarado();
        assertThat(maskedToken).startsWith("eyJhb");
        assertThat(maskedToken).endsWith("ature");
        assertThat(maskedToken).contains("...");
    }

    @Test
    @DisplayName("Deve gerar string segura para logs")
    void testSecureString() {
        RespostaTokenDTO response = RespostaTokenDTO.of("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature", "refresh_token", 3600L);
        
        String secureString = response.toSecureString();
        assertThat(secureString).contains("RespostaTokenDTO");
        assertThat(secureString).contains("...");
        assertThat(secureString).doesNotContain("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.test.signature");
    }
}
