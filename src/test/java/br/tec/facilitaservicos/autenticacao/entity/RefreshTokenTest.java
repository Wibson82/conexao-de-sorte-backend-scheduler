package br.tec.facilitaservicos.autenticacao.entity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testes da entidade RefreshToken")
class RefreshTokenTest {

    @Test
    @DisplayName("Deve criar RefreshToken com valores padrão")
    void testRefreshTokenCreation() {
        RefreshToken token = new RefreshToken();
        
        assertThat(token.isAtivo()).isTrue();
        assertThat(token.isRevogado()).isFalse();
    }

    @Test
    @DisplayName("Deve definir e recuperar propriedades")
    void testSettersAndGetters() {
        RefreshToken token = new RefreshToken();
        LocalDateTime now = LocalDateTime.now();
        
        token.setId(1L);
        token.setTokenHash("hash123");
        token.setUsuarioId(100L);
        token.setAtivo(false);
        token.setRevogado(true);
        token.setDataCriacao(now);
        token.setDataExpiracao(now.plusDays(30));
        token.setUserAgent("Mozilla/5.0");
        
        assertThat(token.getId()).isEqualTo(1L);
        assertThat(token.getTokenHash()).isEqualTo("hash123");
        assertThat(token.getUsuarioId()).isEqualTo(100L);
        assertThat(token.isAtivo()).isFalse();
        assertThat(token.isRevogado()).isTrue();
        assertThat(token.getDataCriacao()).isEqualTo(now);
        assertThat(token.getDataExpiracao()).isEqualTo(now.plusDays(30));
        assertThat(token.getUserAgent()).isEqualTo("Mozilla/5.0");
    }

    @Test
    @DisplayName("Deve verificar se token está expirado")
    void testTokenExpiration() {
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setDataExpiracao(LocalDateTime.now().minusDays(1));
        
        RefreshToken validToken = new RefreshToken();
        validToken.setDataExpiracao(LocalDateTime.now().plusDays(1));
        
        assertThat(expiredToken.isExpirado()).isTrue();
        assertThat(validToken.isExpirado()).isFalse();
    }

    @Test
    @DisplayName("Deve verificar se token está válido")
    void testTokenValidity() {
        RefreshToken validToken = new RefreshToken();
        validToken.setAtivo(true);
        validToken.setRevogado(false);
        validToken.setDataExpiracao(LocalDateTime.now().plusDays(1));
        
        RefreshToken invalidToken = new RefreshToken();
        invalidToken.setAtivo(false);
        invalidToken.setRevogado(true);
        invalidToken.setDataExpiracao(LocalDateTime.now().minusDays(1));
        
        assertThat(validToken.isValido()).isTrue();
        assertThat(invalidToken.isValido()).isFalse();
    }

    @Test
    @DisplayName("Deve verificar equals e hashCode")
    void testEqualsAndHashCode() {
        RefreshToken token1 = new RefreshToken();
        token1.setId(1L);
        token1.setTokenHash("hash123");
        
        RefreshToken token2 = new RefreshToken();
        token2.setId(1L);
        token2.setTokenHash("hash123");
        
        RefreshToken token3 = new RefreshToken();
        token3.setId(2L);
        token3.setTokenHash("hash456");
        
        assertThat(token1).isEqualTo(token2);
        assertThat(token1).isNotEqualTo(token3);
        assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
    }

    @Test
    @DisplayName("Deve gerar toString sem expor dados sensíveis")
    void testToString() {
        RefreshToken token = new RefreshToken();
        token.setId(1L);
        token.setTokenHash("sensitive_hash");
        token.setUsuarioId(100L);
        
        String toString = token.toString();
        
        assertThat(toString).contains("RefreshToken");
        assertThat(toString).contains("id=1");
        assertThat(toString).contains("usuarioId=100");
        assertThat(toString).doesNotContain("sensitive_hash");
    }
}
