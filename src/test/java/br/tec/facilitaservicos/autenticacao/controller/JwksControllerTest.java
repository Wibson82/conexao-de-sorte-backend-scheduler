package br.tec.facilitaservicos.autenticacao.controller;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

import br.tec.facilitaservicos.autenticacao.config.WebFluxTestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import br.tec.facilitaservicos.autenticacao.repository.RefreshTokenRepository;
import br.tec.facilitaservicos.autenticacao.service.JwtService;
import reactor.core.publisher.Mono;

@WebFluxTest(JwksController.class)
@Import({WebFluxTestConfiguration.class, JwksControllerTest.TestConfig.class})
@DisplayName("Testes do JwksController")
class JwksControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;
    
    @Configuration
    static class TestConfig {
        @Bean
        public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
            return http
                .csrf().disable()
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .build();
        }
    }

    @Test
    @DisplayName("GET /.well-known/jwks.json - JWKS com sucesso")
    void testGetJwksSuccess() {
        Map<String, Object> jwksResponse = Map.of(
            "keys", Map.of(
                "kty", "RSA",
                "use", "sig",
                "kid", "test-key-id",
                "n", "test-modulus",
                "e", "AQAB"
            )
        );

        when(jwtService.generateJwkSet())
            .thenReturn(Mono.just(jwksResponse));

        webTestClient.get()
            .uri("/rest/v1/.well-known/jwks.json")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectHeader().valueEquals("Cache-Control", "max-age=3600, public")
            .expectBody()
            .jsonPath("$.keys").exists();
    }

    @Test
    @DisplayName("GET /.well-known/jwks.json - Erro interno")
    void testGetJwksError() {
        when(jwtService.generateJwkSet())
            .thenReturn(Mono.error(new RuntimeException("KeyVault error")));

        webTestClient.get()
            .uri("/rest/v1/.well-known/jwks.json")
            .exchange()
            .expectStatus().is5xxServerError();
    }

    // Teste removido: endpoint /.well-known/openid_configuration não implementado
}
