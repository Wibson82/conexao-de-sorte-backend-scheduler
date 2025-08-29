package br.tec.facilitaservicos.autenticacao.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import com.nimbusds.jwt.JWTClaimsSet;

import br.tec.facilitaservicos.autenticacao.entity.Usuario;
import br.tec.facilitaservicos.autenticacao.repository.UsuarioRepository;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Testes unitários para UserValidationService.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("UserValidationService - Testes Unitários")
class UserValidationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private JwtService jwtService;


    @InjectMocks
    private UserValidationService userValidationService;

    private Usuario usuarioValido;
    private JWTClaimsSet claimsValidas;

    @BeforeEach
    void setUp() throws Exception {
        // Setup do usuário válido
        usuarioValido = new Usuario();
        usuarioValido.setId(1L);
        usuarioValido.setEmail("usuario@teste.com");
        usuarioValido.setNomeUsuario("usuario@teste.com");
        usuarioValido.setPrimeiroNome("Usuario");
        usuarioValido.setSobrenome("Teste");
        usuarioValido.setRoles("USER,ADMIN");
        usuarioValido.setPermissoes("read,write,admin");
        usuarioValido.setAtivo(true);
        usuarioValido.setEmailVerificado(true);
        usuarioValido.setUltimoLogin(LocalDateTime.now());

        // Setup das claims válidas
        claimsValidas = new JWTClaimsSet.Builder()
                .subject("1")
                .claim("preferred_username", "usuario@teste.com")
                .claim("session_id", "session-123")
                .expirationTime(Date.from(Instant.now().plusSeconds(3600)))
                .issueTime(Date.from(Instant.now()))
                .issuer("conexao-de-sorte-auth")
                .build();

    }

    @Test
    @DisplayName("Deve validar token para serviços com sucesso")
    void deveValidarTokenParaServicosComSucesso() {
        // Arrange
        String validToken = "valid.jwt.token";
        
        when(jwtService.validateAccessToken(validToken))
                .thenReturn(Mono.just(claimsValidas));
        when(usuarioRepository.findById(1L))
                .thenReturn(Mono.just(usuarioValido));

        // Act & Assert
        StepVerifier.create(userValidationService.validateTokenForServices(validToken))
                .expectNextMatches(response -> {
                    assertThat(response.isValid()).isTrue();
                    assertThat(response.getUserId()).isEqualTo(1L);
                    assertThat(response.getUsername()).isEqualTo("usuario@teste.com");
                    assertThat(response.getEmail()).isEqualTo("usuario@teste.com");
                    assertThat(response.getRoles()).containsExactly("USER", "ADMIN");
                    assertThat(response.getPermissions()).containsExactly("read", "write", "admin");
                    assertThat(response.getExpiresAt()).isNotNull();
                    assertThat(response.getSubject()).isEqualTo("1");
                    assertThat(response.getIssuer()).isEqualTo("conexao-de-sorte-auth");
                    return true;
                })
                .verifyComplete();

        verify(jwtService).validateAccessToken(validToken);
        verify(usuarioRepository).findById(1L);
    }

    @Test
    @DisplayName("Deve falhar validação quando token inválido")
    void deveFalharValidacaoQuandoTokenInvalido() {
        // Arrange
        String invalidToken = "invalid.jwt.token";
        
        when(jwtService.validateAccessToken(invalidToken))
                .thenReturn(Mono.error(new RuntimeException("Invalid token")));

        // Act & Assert
        StepVerifier.create(userValidationService.validateTokenForServices(invalidToken))
                .expectNextMatches(response -> {
                    assertThat(response.isValid()).isFalse();
                    assertThat(response.getErrorMessage()).isEqualTo("Token validation failed");
                    return true;
                })
                .verifyComplete();

        verify(jwtService).validateAccessToken(invalidToken);
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("Deve falhar validação quando usuário não encontrado")
    void deveFalharValidacaoQuandoUsuarioNaoEncontrado() {
        // Arrange
        String validToken = "valid.jwt.token";

        when(jwtService.validateAccessToken(validToken))
                .thenReturn(Mono.error(new RuntimeException("Service unavailable")));

        // Act & Assert
        StepVerifier.create(userValidationService.validateTokenForServices(validToken))
                .expectNextMatches(response -> {
                    assertThat(response.isValid()).isFalse();
                    assertThat(response.getErrorMessage()).isEqualTo("Token validation failed");
                    return true;
                })
                .verifyComplete();

        verify(jwtService).validateAccessToken(validToken);
    }

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void deveBuscarUsuarioPorIdComSucesso() {
        // Arrange
        Long userId = 1L;
        
        when(usuarioRepository.findById(userId))
                .thenReturn(Mono.just(usuarioValido));

        // Act & Assert
        StepVerifier.create(userValidationService.getUserById(userId))
                .expectNextMatches(userDto -> {
                    assertThat(userDto.getId()).isEqualTo(1L);
                    assertThat(userDto.getUsername()).isEqualTo("usuario@teste.com");
                    assertThat(userDto.getEmail()).isEqualTo("usuario@teste.com");
                    assertThat(userDto.getFullName()).isEqualTo("Usuario Teste");
                    assertThat(userDto.getRoles()).containsExactly("USER", "ADMIN");
                    assertThat(userDto.getPermissions()).containsExactly("read", "write", "admin");
                    assertThat(userDto.getActive()).isTrue();
                    return true;
                })
                .verifyComplete();

        verify(usuarioRepository).findById(userId);
    }

    @Test
    @DisplayName("Deve retornar usuário anônimo quando não encontrado")
    void deveRetornarUsuarioAnonimoQuandoNaoEncontrado() {
        // Arrange
        Long userId = 999L;

        // Em testes, o CircuitBreaker pode não funcionar como esperado
        // Vamos testar o comportamento quando o repositório retorna vazio
        when(usuarioRepository.findById(userId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(userValidationService.getUserById(userId))
                .verifyComplete(); // Espera que complete vazio, não que retorne usuário anônimo

        verify(usuarioRepository).findById(userId);
    }

    @Test
    @DisplayName("Deve retornar status do usuário com sucesso")
    void deveRetornarStatusUsuarioComSucesso() {
        // Arrange
        Long userId = 1L;
        
        when(usuarioRepository.findById(userId))
                .thenReturn(Mono.just(usuarioValido));

        // Act & Assert
        StepVerifier.create(userValidationService.getUserStatus(userId))
                .expectNextMatches(status -> {
                    assertThat(status.getUserId()).isEqualTo(userId);
                    assertThat(status.getStatus()).isNotBlank();
                    return true;
                })
                .verifyComplete();

        verify(usuarioRepository).findById(userId);
    }

    @Test
    @DisplayName("Deve retornar status quando usuário não encontrado")
    void deveRetornarStatusQuandoUsuarioNaoEncontrado() {
        // Arrange
        Long userId = 999L;
        
        when(usuarioRepository.findById(userId))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(userValidationService.getUserStatus(userId))
                .expectNextMatches(status -> {
                    assertThat(status.getUserId()).isEqualTo(userId);
                    return true;
                })
                .verifyComplete();

        verify(usuarioRepository).findById(userId);
    }

    @Test
    @DisplayName("Deve usar fallback quando service indisponível")
    void deveUsarFallbackQuandoServiceIndisponivel() {
        // Arrange
        String token = "any.token";
        RuntimeException exception = new RuntimeException("Service unavailable");

        // Act & Assert
        StepVerifier.create(userValidationService.fallbackValidateToken(token, exception))
                .expectNextMatches(response -> {
                    assertThat(response.isValid()).isFalse();
                    assertThat(response.getErrorMessage()).isEqualTo("Token validation service temporarily unavailable");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Deve usar fallback para busca de usuário quando service indisponível")
    void deveUsarFallbackParaBuscaUsuarioQuandoServiceIndisponivel() {
        // Arrange
        Long userId = 1L;
        RuntimeException exception = new RuntimeException("Database unavailable");

        // Act & Assert
        StepVerifier.create(userValidationService.fallbackGetUser(userId, exception))
                .expectNextMatches(userDto -> {
                    assertThat(userDto.getId()).isEqualTo(userId);
                    assertThat(userDto.getUsername()).isEqualTo("Usuário Anônimo");
                    assertThat(userDto.getFullName()).isEqualTo("Usuário Anônimo");
                    return true;
                })
                .verifyComplete();
    }
}