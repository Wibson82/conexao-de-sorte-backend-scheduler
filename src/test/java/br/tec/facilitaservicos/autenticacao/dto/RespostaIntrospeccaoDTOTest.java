package br.tec.facilitaservicos.autenticacao.dto;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes do RespostaIntrospeccaoDTO")
class RespostaIntrospeccaoDTOTest {

    @Test
    @DisplayName("Deve criar resposta de introspecção ativa")
    void testActiveIntrospectionResponse() {
        Set<String> authorities = Set.of("read", "write");
        RespostaIntrospeccaoDTO response = RespostaIntrospeccaoDTO.ativo(
            "user@test.com",
            "https://auth.test.com",
            "test-audience",
            System.currentTimeMillis() / 1000 + 3600,
            System.currentTimeMillis() / 1000,
            authorities
        );
        
        assertThat(response.ativo()).isTrue();
        assertThat(response.subject()).isEqualTo("user@test.com");
        assertThat(response.emissor()).isEqualTo("https://auth.test.com");
        assertThat(response.audiencia()).isEqualTo("test-audience");
        assertThat(response.autoridades()).containsExactlyInAnyOrder("read", "write");
    }

    @Test
    @DisplayName("Deve criar resposta de introspecção inativa")
    void testInactiveIntrospectionResponse() {
        RespostaIntrospeccaoDTO response = RespostaIntrospeccaoDTO.inativo();
        
        assertThat(response.ativo()).isFalse();
        assertThat(response.subject()).isNull();
        assertThat(response.emissor()).isNull();
        assertThat(response.audiencia()).isNull();
        assertThat(response.autoridades()).isNull();
    }

    @Test
    @DisplayName("Deve verificar se token está expirado")
    void testTokenExpiration() {
        long pastTimestamp = System.currentTimeMillis() / 1000 - 3600; // 1 hora atrás
        RespostaIntrospeccaoDTO response = RespostaIntrospeccaoDTO.ativo(
            "user@test.com",
            "https://auth.test.com",
            "test-audience",
            pastTimestamp,
            System.currentTimeMillis() / 1000,
            Set.of("read")
        );
        
        assertThat(response.isExpirado()).isTrue();
    }

    @Test
    @DisplayName("Deve converter timestamp para LocalDateTime")
    void testTimestampConversion() {
        long currentTimestamp = System.currentTimeMillis() / 1000;
        RespostaIntrospeccaoDTO response = RespostaIntrospeccaoDTO.ativo(
            "user@test.com",
            "https://auth.test.com",
            "test-audience",
            currentTimestamp + 3600,
            currentTimestamp,
            Set.of("read")
        );
        
        assertThat(response.getDataExpiracao()).isNotNull();
        assertThat(response.getDataEmissao()).isNotNull();
    }

    @Test
    @DisplayName("Deve retornar null para timestamps null")
    void testNullTimestamps() {
        RespostaIntrospeccaoDTO response = new RespostaIntrospeccaoDTO(
            true, "user", "issuer", "audience", null, null, 
            "read write", "client", "Bearer", "user", Set.of("read")
        );
        
        assertThat(response.getDataExpiracao()).isNull();
        assertThat(response.getDataEmissao()).isNull();
        assertThat(response.isExpirado()).isFalse();
    }
}
