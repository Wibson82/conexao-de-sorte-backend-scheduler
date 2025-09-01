package br.tec.facilitaservicos.autenticacao.controller;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import br.tec.facilitaservicos.autenticacao.dto.RequisicaoIntrospeccaoDTO;
import br.tec.facilitaservicos.autenticacao.dto.RequisicaoLoginDTO;
import br.tec.facilitaservicos.autenticacao.dto.RequisicaoRefreshDTO;
import br.tec.facilitaservicos.autenticacao.dto.RespostaIntrospeccaoDTO;
import br.tec.facilitaservicos.autenticacao.dto.RespostaTokenDTO;
import br.tec.facilitaservicos.autenticacao.exception.AuthenticationException;
import br.tec.facilitaservicos.autenticacao.service.AuthService;
import reactor.core.publisher.Mono;

@WebFluxTest(AuthController.class)
@DisplayName("Testes do AuthController")
class AuthControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AuthService authService;

    private RequisicaoLoginDTO validLoginRequest;
    private RequisicaoRefreshDTO validRefreshRequest;
    private RequisicaoIntrospeccaoDTO validIntrospectionRequest;
    private RespostaTokenDTO tokenResponse;
    private RespostaIntrospeccaoDTO introspectionResponse;

    @BeforeEach
    void setUp() {
        validLoginRequest = new RequisicaoLoginDTO("user@test.com", "password123");
        validRefreshRequest = new RequisicaoRefreshDTO("valid_refresh_token");
        validIntrospectionRequest = new RequisicaoIntrospeccaoDTO("valid_access_token", "access_token");
        
        tokenResponse = RespostaTokenDTO.of("access_token_value", "refresh_token_value", 3600L, Set.of("read", "write"));
        
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
            .uri("/api/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validLoginRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.access_token").isEqualTo("access_token_value")
            .jsonPath("$.refresh_token").isEqualTo("refresh_token_value")
            .jsonPath("$.token_type").isEqualTo("Bearer")
            .jsonPath("$.expires_in").isEqualTo(3600);
    }

    @Test
    @DisplayName("POST /api/v1/auth/token - Login com credenciais inválidas")
    void testLoginInvalidCredentials() {
        when(authService.authenticate(any(RequisicaoLoginDTO.class), anyString(), anyString()))
            .thenReturn(Mono.error(new AuthenticationException("Credenciais inválidas")));

        webTestClient.post()
            .uri("/api/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validLoginRequest)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /api/v1/auth/token - Requisição inválida")
    void testLoginInvalidRequest() {
        RequisicaoLoginDTO invalidRequest = new RequisicaoLoginDTO("", "");

        webTestClient.post()
            .uri("/api/v1/auth/token")
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
            .uri("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validRefreshRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.access_token").isEqualTo("access_token_value")
            .jsonPath("$.refresh_token").isEqualTo("refresh_token_value");
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Refresh token inválido")
    void testRefreshTokenInvalid() {
        when(authService.refresh(any(RequisicaoRefreshDTO.class), anyString(), anyString()))
            .thenReturn(Mono.error(new AuthenticationException("Refresh token inválido")));

        webTestClient.post()
            .uri("/api/v1/auth/refresh")
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
            .uri("/api/v1/auth/introspect")
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
            .uri("/api/v1/auth/introspect")
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
            .uri("/api/v1/auth/revoke")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validIntrospectionRequest)
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("GET /api/v1/auth/status - Health check")
    void testHealthCheck() {
        webTestClient.get()
            .uri("/api/v1/auth/status")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.status").isEqualTo("healthy")
            .jsonPath("$.service").isEqualTo("auth-service");
    }
}
