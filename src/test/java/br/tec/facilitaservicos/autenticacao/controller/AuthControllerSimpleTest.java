package br.tec.facilitaservicos.autenticacao.controller;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.reset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import br.tec.facilitaservicos.autenticacao.dto.RequisicaoLoginDTO;
import br.tec.facilitaservicos.autenticacao.dto.RespostaTokenDTO;
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
@DisplayName("Teste simples do AuthController")
class AuthControllerSimpleTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private R2dbcEntityTemplate r2dbcEntityTemplate;

    @MockitoBean
    private R2dbcMappingContext r2dbcMappingContext;

    private RequisicaoLoginDTO validLoginRequest;
    private RespostaTokenDTO tokenResponse;

    @BeforeEach
    void setUp() {
        reset(authService, refreshTokenRepository, r2dbcEntityTemplate, r2dbcMappingContext);
        
        validLoginRequest = new RequisicaoLoginDTO("user@test.com", "password123");
        tokenResponse = RespostaTokenDTO.of(
            "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQHRlc3QuY29tIiwiaWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDM2MDB9.signature", 
            "refresh_token_value", 
            3600L, 
            Set.of("read", "write")
        );
    }

    @Test
    @DisplayName("POST /rest/v1/auth/token - Teste de endpoint disponível")
    void testTokenEndpointExists() {
        // Teste simples para verificar se o endpoint existe
        webTestClient.post()
            .uri("/rest/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validLoginRequest)
            .exchange()
            .expectStatus().is5xxServerError(); // Esperamos erro 500 por enquanto
    }

    @Test
    @DisplayName("POST /rest/v1/auth/token - Login com sucesso (teste com mock)")
    void testLoginSuccessWithMock() {
        // Configurar o mock para retornar o token response
        when(authService.authenticate(any(RequisicaoLoginDTO.class), anyString(), anyString()))
            .thenReturn(Mono.just(tokenResponse));

        // Executar a requisição e verificar a resposta
        webTestClient.post()
            .uri("/rest/v1/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(validLoginRequest)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.access_token").exists()
            .jsonPath("$.refresh_token").exists();

        // Verificar se o mock foi chamado
        verify(authService, times(1)).authenticate(any(RequisicaoLoginDTO.class), anyString(), anyString());
    }
}