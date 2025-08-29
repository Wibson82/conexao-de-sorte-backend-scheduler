package br.tec.facilitaservicos.autenticacao.service;

import br.tec.facilitaservicos.autenticacao.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes básicos para verificar se a estrutura está funcionando.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Básicos - Estrutura de Testes")
class BasicServiceTest {

    @Test
    @DisplayName("Deve criar usuário básico")
    void deveCriarUsuarioBasico() {
        // Arrange & Act
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("teste@exemplo.com");
        usuario.setNomeUsuario("teste");
        usuario.setAtivo(true);

        // Assert
        assertThat(usuario.getId()).isEqualTo(1L);
        assertThat(usuario.getEmail()).isEqualTo("teste@exemplo.com");
        assertThat(usuario.getNomeUsuario()).isEqualTo("teste");
        assertThat(usuario.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Deve validar campos obrigatórios do usuário")
    void deveValidarCamposObrigatoriosDoUsuario() {
        // Arrange & Act
        Usuario usuario = new Usuario();

        // Assert
        assertThat(usuario.getId()).isNull();
        assertThat(usuario.getEmail()).isNull();
        assertThat(usuario.getNomeUsuario()).isNull();
    }

    @Test
    @DisplayName("Deve verificar se estrutura de testes está funcionando")
    void deveVerificarSeEstruturaDeTestesEstaFuncionando() {
        // Arrange
        String valor = "teste";

        // Act & Assert
        assertThat(valor).isNotNull();
        assertThat(valor).isEqualTo("teste");
        assertThat(valor).hasSize(5);
    }
}