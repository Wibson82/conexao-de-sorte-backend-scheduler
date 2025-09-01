package br.tec.facilitaservicos.autenticacao.controller;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import br.tec.facilitaservicos.autenticacao.service.JwtService;
import reactor.core.publisher.Mono;

@WebFluxTest(JwksController.class)
@DisplayName("Testes do JwksController")
class JwksControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private JwtService jwtService;

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
            .uri("/.well-known/jwks.json")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectHeader().valueEquals("Cache-Control", "public, max-age=3600")
            .expectBody()
            .jsonPath("$.keys").exists();
    }

    @Test
    @DisplayName("GET /.well-known/jwks.json - Erro interno")
    void testGetJwksError() {
        when(jwtService.generateJwkSet())
            .thenReturn(Mono.error(new RuntimeException("KeyVault error")));

        webTestClient.get()
            .uri("/.well-known/jwks.json")
            .exchange()
            .expectStatus().is5xxServerError();
    }

    @Test
    @DisplayName("GET /.well-known/openid_configuration - OpenID Configuration")
    void testGetOpenIdConfiguration() {
        webTestClient.get()
            .uri("/.well-known/openid_configuration")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.issuer").exists()
            .jsonPath("$.jwks_uri").exists()
            .jsonPath("$.token_endpoint").exists()
            .jsonPath("$.introspection_endpoint").exists()
            .jsonPath("$.revocation_endpoint").exists();
    }
}
