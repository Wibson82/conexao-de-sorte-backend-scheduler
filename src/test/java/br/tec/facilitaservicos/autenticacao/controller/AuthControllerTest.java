package br.tec.facilitaservicos.autenticacao.controller;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import br.tec.facilitaservicos.autenticacao.config.WebFluxTestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import br.tec.facilitaservicos.autenticacao.dto.RequisicaoIntrospeccaoDTO;
import br.tec.facilitaservicos.autenticacao.dto.RequisicaoLoginDTO;
import br.tec.facilitaservicos.autenticacao.dto.RequisicaoRefreshDTO;
import br.tec.facilitaservicos.autenticacao.dto.RespostaIntrospeccaoDTO;
import br.tec.facilitaservicos.autenticacao.dto.RespostaTokenDTO;
import br.tec.facilitaservicos.autenticacao.exception.AuthenticationException;
import br.tec.facilitaservicos.autenticacao.repository.RefreshTokenRepository;
import br.tec.facilitaservicos.autenticacao.service.AuthService;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = AuthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration.class,
    org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration.class
})
@Import({AuthControllerTest.TestConfig.class})
@DisplayName("Testes do AuthController")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;
    
    @Configuration
    static class TestConfig {
        @Bean
        public SecurityWebFilterChain testSecurityFilterChain(ServerHttpSecurity http) {
            return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
        }
        
        @Bean
        @Primary
        public AuthController authController(AuthService authService) {
            return new AuthController(authService);
        }
    }

    private RequisicaoLoginDTO validLoginRequest;
    private RequisicaoRefreshDTO validRefreshRequest;
    private RequisicaoIntrospeccaoDTO validIntrospectionRequest;
    private RespostaTokenDTO tokenResponse;
    private RespostaIntrospeccaoDTO introspectionResponse;

    @BeforeEach
    void setUp() {
        reset(authService, refreshTokenRepository);
        
        validLoginRequest = new RequisicaoLoginDTO("user@test.com", "password123");
        validRefreshRequest = new RequisicaoRefreshDTO("valid_refresh_token");
        validIntrospectionRequest = new RequisicaoIntrospeccaoDTO("valid_access_token", "access_token");
        
        tokenResponse = RespostaTokenDTO.of("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQHRlc3QuY29tIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDM2MDB9.signature", "refresh_token_value", 3600L, Set.of("read", "write"));
        
        introspectionResponse = RespostaIntrospeccaoDTO.ativo(
            "user@test.com",
            "https://auth.test.com",
            "test-audience",
            System.currentTimeMillis() / 1000 + 3600,
            System.currentTimeMillis() / 1000,
            Set.of("read", "write")
        );
    }

    @Test
    @DisplayName("POST /api/v1/auth/token - Login com sucesso")
    void testLoginSuccess() {
        when(authService.authenticate(any(RequisicaoLoginDTO.class), anyString(), anyString()))
            .thenReturn(Mono.just(tokenResponse));

        webTestClient.post()
            .uri("/rest/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validLoginRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.access_token").exists()
            .jsonPath("$.refresh_token").exists()
            .jsonPath("$.expires_in").isEqualTo(3600);
    }

    @Test
    @DisplayName("POST /api/v1/auth/token - Login com credenciais inválidas")
    void testLoginInvalidCredentials() {
        when(authService.authenticate(any(RequisicaoLoginDTO.class), anyString(), anyString()))
            .thenReturn(Mono.error(new AuthenticationException("Credenciais inválidas")));

        webTestClient.post()
            .uri("/rest/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validLoginRequest)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/auth/token - Requisição inválida")
    void testLoginInvalidRequest() {
        // Usando um Map para simular JSON inválido, evitando a validação do construtor do DTO
        java.util.Map<String, String> invalidRequest = java.util.Map.of(
            "username", "",
            "password", ""
        );

        webTestClient.post()
            .uri("/rest/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(invalidRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Refresh token com sucesso")
    void testRefreshTokenSuccess() {
        when(authService.refresh(any(RequisicaoRefreshDTO.class), anyString(), anyString()))
            .thenReturn(Mono.just(tokenResponse));

        webTestClient.post()
            .uri("/rest/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validRefreshRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.access_token").exists()
            .jsonPath("$.refresh_token").exists();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Refresh token inválido")
    void testRefreshTokenInvalid() {
        when(authService.refresh(any(RequisicaoRefreshDTO.class), anyString(), anyString()))
            .thenReturn(Mono.error(new AuthenticationException("Refresh token inválido")));

        webTestClient.post()
            .uri("/rest/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validRefreshRequest)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/auth/introspect - Introspecção com sucesso")
    void testIntrospectSuccess() {
        when(authService.introspect(anyString()))
            .thenReturn(Mono.just(introspectionResponse));

        webTestClient.post()
            .uri("/rest/v1/auth/introspect")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validIntrospectionRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.active").isEqualTo(true)
            .jsonPath("$.token_type").isEqualTo("Bearer")
            .jsonPath("$.sub").isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("POST /api/v1/auth/introspect - Token inválido")
    void testIntrospectInvalidToken() {
        RespostaIntrospeccaoDTO inactiveResponse = RespostaIntrospeccaoDTO.inativo();
        
        when(authService.introspect(anyString()))
            .thenReturn(Mono.just(inactiveResponse));

        webTestClient.post()
            .uri("/rest/v1/auth/introspect")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validIntrospectionRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.active").isEqualTo(false);
    }

    @Test
    @DisplayName("POST /api/v1/auth/revoke - Revogação com sucesso")
    void testRevokeSuccess() {
        when(authService.revoke(anyString()))
            .thenReturn(Mono.empty());

        webTestClient.post()
            .uri("/rest/v1/auth/revoke")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validRefreshRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.revoked").isEqualTo(true)
            .jsonPath("$.message").isEqualTo("Token revogado com sucesso");
    }

    @Test
    @DisplayName("GET /api/v1/auth/status - Health check")
    void testHealthCheck() {
        // Mock do health check
        when(authService.healthCheck()).thenReturn(Mono.just(true));
        
        webTestClient.get()
            .uri("/rest/v1/auth/health")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.service").isEqualTo("authentication");
    }
}
