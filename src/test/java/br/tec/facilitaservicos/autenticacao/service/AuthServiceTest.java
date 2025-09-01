package br.tec.facilitaservicos.autenticacao.service;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import br.tec.facilitaservicos.autenticacao.dto.RequisicaoLoginDTO;
import br.tec.facilitaservicos.autenticacao.dto.RequisicaoRefreshDTO;
import br.tec.facilitaservicos.autenticacao.entity.RefreshToken;
import br.tec.facilitaservicos.autenticacao.dto.UsuarioDTO;
import br.tec.facilitaservicos.autenticacao.exception.AuthenticationException;
import br.tec.facilitaservicos.autenticacao.repository.RefreshTokenRepository;
import br.tec.facilitaservicos.autenticacao.client.UserServiceClient;
import java.util.Set;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Testes unitários para AuthService.
 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("AuthService - Testes Unitários")
class AuthServiceTest {

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserValidationService userValidationService;

    @InjectMocks
    private AuthService authService;

    private UsuarioDTO usuarioValido;
    private RequisicaoLoginDTO requisicaoLoginValida;

    @BeforeEach
    void setUp() {
        // Setup do usuário válido
        usuarioValido = new UsuarioDTO();
        usuarioValido.setId(1L);
        usuarioValido.setEmail("usuario@teste.com");
        usuarioValido.setUsername("usuario@teste.com");
        usuarioValido.setPassword("$2a$10$hashSenha");
        usuarioValido.setAtivo(true);
        usuarioValido.setEmailVerificado(true);
        usuarioValido.setContaBloqueada(false);
        usuarioValido.setTentativasLoginFalidas(0);

        // Setup da requisição de login válida
        requisicaoLoginValida = new RequisicaoLoginDTO("usuario@teste.com", "senha123");
    }

    @Test
    @DisplayName("Deve realizar autenticação com sucesso quando credenciais válidas")
    void deveRealizarAutenticacaoComSucessoQuandoCredenciaisValidas() {
        // Arrange
        String accessToken = "access.token.jwt";
        String refreshTokenValue = "refresh.token.uuid";
        
        when(userServiceClient.findByEmailOrNomeUsuario(anyString()))
                .thenReturn(Mono.just(usuarioValido));
        when(userValidationService.validateUser(any(UsuarioDTO.class), anyString()))
                .thenReturn(Mono.just(usuarioValido));
        when(jwtService.generateAccessToken(any(UsuarioDTO.class)))
                .thenReturn(Mono.just(accessToken));
        // Refresh token é gerado internamente no AuthService
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(Mono.just(new RefreshToken()));
        // Mock para updateTentativasLoginFalidas - corrige NullPointerException
        when(userServiceClient.updateTentativasLoginFalidas(anyLong(), anyInt()))
                .thenReturn(Mono.just(1));

        // Act & Assert
        StepVerifier.create(authService.authenticate(requisicaoLoginValida, "192.168.1.1", "test-agent"))
                .expectNextMatches(resposta -> {
                    return resposta.tokenAcesso().equals(accessToken) &&
                           resposta.tokenRenovacao() != null &&
                           "Bearer".equals(resposta.tipoToken()) &&
                           resposta.tempoValidadeSegundos() > 0;
                })
                .verifyComplete();

        // Verificações adicionais
        verify(userServiceClient).findByEmailOrNomeUsuario("usuario@teste.com");
        verify(userValidationService).validateUser(usuarioValido, "senha123");
        verify(jwtService).generateAccessToken(usuarioValido);
        verify(userServiceClient).updateTentativasLoginFalidas(usuarioValido.getId(), 0);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Deve falhar autenticação quando usuário não encontrado")
    void deveFalharAutenticacaoQuandoUsuarioNaoEncontrado() {
        // Arrange
        when(userServiceClient.findByEmailOrNomeUsuario(anyString()))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(authService.authenticate(requisicaoLoginValida, "192.168.1.1", "test-agent"))
                .expectError(AuthenticationException.class)
                .verify();

        verify(userServiceClient).findByEmailOrNomeUsuario("usuario@teste.com");
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Deve falhar autenticação quando senha incorreta")
    void deveFalharAutenticacaoQuandoSenhaIncorreta() {
        // Arrange
        when(userServiceClient.findByEmailOrNomeUsuario(anyString()))
                .thenReturn(Mono.just(usuarioValido));
        when(userValidationService.validateUser(any(UsuarioDTO.class), anyString()))
                .thenReturn(Mono.error(new AuthenticationException("Senha inválida")));

        // Act & Assert
        StepVerifier.create(authService.authenticate(requisicaoLoginValida, "192.168.1.1", "test-agent"))
                .expectError(AuthenticationException.class)
                .verify();

        verify(userServiceClient).findByEmailOrNomeUsuario("usuario@teste.com");
        verify(userValidationService).validateUser(usuarioValido, "senha123");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Deve falhar autenticação quando conta bloqueada")
    void deveFalharAutenticacaoQuandoContaBloqueada() {
        // Arrange
        usuarioValido.setContaBloqueada(true);
        usuarioValido.setDataBloqueio(LocalDateTime.now().minusMinutes(10));

        when(userServiceClient.findByEmailOrNomeUsuario(anyString()))
                .thenReturn(Mono.just(usuarioValido));
        when(userValidationService.validateUser(any(UsuarioDTO.class), anyString()))
                .thenReturn(Mono.error(new AuthenticationException("Conta bloqueada")));

        // Act & Assert
        StepVerifier.create(authService.authenticate(requisicaoLoginValida, "192.168.1.1", "test-agent"))
                .expectError(AuthenticationException.class)
                .verify();

        verify(userServiceClient).findByEmailOrNomeUsuario("usuario@teste.com");
        verify(userValidationService).validateUser(usuarioValido, "senha123");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Deve falhar autenticação quando usuário inativo")
    void deveFalharAutenticacaoQuandoUsuarioInativo() {
        // Arrange
        usuarioValido.setAtivo(false);

        when(userServiceClient.findByEmailOrNomeUsuario(anyString()))
                .thenReturn(Mono.just(usuarioValido));
        when(userValidationService.validateUser(any(UsuarioDTO.class), anyString()))
                .thenReturn(Mono.error(new AuthenticationException("Conta inativa")));

        // Act & Assert
        StepVerifier.create(authService.authenticate(requisicaoLoginValida, "192.168.1.1", "test-agent"))
                .expectError(AuthenticationException.class)
                .verify();

        verify(userServiceClient).findByEmailOrNomeUsuario("usuario@teste.com");
        verify(userValidationService).validateUser(usuarioValido, "senha123");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Deve falhar autenticação quando email não verificado")
    void deveFalharAutenticacaoQuandoEmailNaoVerificado() {
        // Arrange
        usuarioValido.setEmailVerificado(false);

        when(userServiceClient.findByEmailOrNomeUsuario(anyString()))
                .thenReturn(Mono.just(usuarioValido));
        when(userValidationService.validateUser(any(UsuarioDTO.class), anyString()))
                .thenReturn(Mono.error(new AuthenticationException("Email não verificado")));

        // Act & Assert
        StepVerifier.create(authService.authenticate(requisicaoLoginValida, "192.168.1.1", "test-agent"))
                .expectError(AuthenticationException.class)
                .verify();

        verify(userServiceClient).findByEmailOrNomeUsuario("usuario@teste.com");
        verify(userValidationService).validateUser(usuarioValido, "senha123");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Deve renovar token com sucesso quando refresh token válido")
    void deveRenovarTokenComSucessoQuandoRefreshTokenValido() {
        // Arrange
        String refreshToken = "valid.refresh.token";
        String novoAccessToken = "new.access.token";
        RequisicaoRefreshDTO refreshRequest = new RequisicaoRefreshDTO(refreshToken);
        
        RefreshToken refreshTokenEntity = new RefreshToken();
        refreshTokenEntity.setTokenHash("hashed-token");
        refreshTokenEntity.setUsuarioId(1L);
        refreshTokenEntity.setDataExpiracao(LocalDateTime.now().plusDays(1));
        refreshTokenEntity.setRevogado(false);
        refreshTokenEntity.setAtivo(true);

        when(refreshTokenRepository.findByTokenHashAndAtivoTrueAndRevogadoFalse(anyString()))
                .thenReturn(Mono.just(refreshTokenEntity));
        when(userServiceClient.findById(1L))
                .thenReturn(Mono.just(usuarioValido));
        when(jwtService.generateAccessToken(usuarioValido))
                .thenReturn(Mono.just(novoAccessToken));
        // Refresh token é gerado internamente no AuthService
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenReturn(Mono.just(refreshTokenEntity));
        // Mock para deactivateToken - corrige NullPointerException
        when(refreshTokenRepository.deactivateToken(anyString()))
                .thenReturn(Mono.just(1));

        // Act & Assert
        StepVerifier.create(authService.refresh(refreshRequest, "192.168.1.1", "test-agent"))
                .expectNextMatches(resposta -> {
                    return resposta.tokenAcesso().equals(novoAccessToken) &&
                           resposta.tokenRenovacao() != null &&
                           "Bearer".equals(resposta.tipoToken());
                })
                .verifyComplete();

        verify(refreshTokenRepository).findByTokenHashAndAtivoTrueAndRevogadoFalse(anyString());
        verify(userServiceClient).findById(1L);
        verify(jwtService).generateAccessToken(usuarioValido);
    }

    @Test
    @DisplayName("Deve falhar renovação quando refresh token não encontrado")
    void deveFalharRenovacaoQuandoRefreshTokenNaoEncontrado() {
        // Arrange
        String refreshToken = "invalid.refresh.token";
        RequisicaoRefreshDTO refreshRequest = new RequisicaoRefreshDTO(refreshToken);
        
        when(refreshTokenRepository.findByTokenHashAndAtivoTrueAndRevogadoFalse(anyString()))
                .thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(authService.refresh(refreshRequest, "192.168.1.1", "test-agent"))
                .expectError(AuthenticationException.class)
                .verify();

        verify(refreshTokenRepository).findByTokenHashAndAtivoTrueAndRevogadoFalse(anyString());
        verifyNoInteractions(userServiceClient, jwtService);
    }

    @Test
    @DisplayName("Deve revogar refresh token com sucesso")
    void deveRevogarRefreshTokenComSucesso() {
        // Arrange
        String refreshToken = "token.to.revoke";

        // Mock para revokeToken - corrige NullPointerException
        when(refreshTokenRepository.revokeToken(anyString()))
                .thenReturn(Mono.just(1));

        // Act & Assert
        StepVerifier.create(authService.revoke(refreshToken))
                .verifyComplete();

        verify(refreshTokenRepository).revokeToken(anyString());
    }

    @Test
    @DisplayName("Deve realizar introspecção com token válido")
    void deveRealizarIntrospeccaoComTokenValido() {
        // Arrange
        String token = "valid.jwt.token";
        
        when(jwtService.validateAccessToken(token))
                .thenReturn(Mono.just(createMockJWTClaimsSet()));

        // Act & Assert
        StepVerifier.create(authService.introspect(token))
                .expectNextMatches(resposta -> resposta.ativo())
                .verifyComplete();

        verify(jwtService).validateAccessToken(token);
    }

    @Test
    @DisplayName("Deve retornar token inativo na introspecção quando token inválido")
    void deveRetornarTokenInativoNaIntrospeccaoQuandoTokenInvalido() {
        // Arrange
        String token = "invalid.jwt.token";
        
        when(jwtService.validateAccessToken(token))
                .thenReturn(Mono.error(new RuntimeException("Invalid token")));

        // Act & Assert
        StepVerifier.create(authService.introspect(token))
                .expectNextMatches(resposta -> !resposta.ativo())
                .verifyComplete();

        verify(jwtService).validateAccessToken(token);
    }


    // Método auxiliar para criar mock do JWTClaimsSet
    private com.nimbusds.jwt.JWTClaimsSet createMockJWTClaimsSet() {
        try {
            return new com.nimbusds.jwt.JWTClaimsSet.Builder()
                    .subject("1")
                    .claim("preferred_username", "usuario@teste.com")
                    .expirationTime(new java.util.Date(System.currentTimeMillis() + 3600000))
                    .issueTime(new java.util.Date())
                    .issuer("conexao-de-sorte-auth")
                    .audience("conexao-de-sorte")
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}