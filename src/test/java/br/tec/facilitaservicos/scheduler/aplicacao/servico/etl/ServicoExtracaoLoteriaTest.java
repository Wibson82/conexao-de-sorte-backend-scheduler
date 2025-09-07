package br.tec.facilitaservicos.scheduler.aplicacao.servico.etl;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Testes de integração para ServicoExtracaoLoteria com WireMock.
 */
@SpringBootTest
@ActiveProfiles("test")
class ServicoExtracaoLoteriaTest {

    private WireMockServer wireMockServer;
    private ServicoExtracaoLoteria servicoExtracao;
    private ProcessadorExtracaoResultado processadorExtracao;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig()
                .port(0)); // Porta aleatória
        wireMockServer.start();
        
        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(wireMockServer.baseUrl());
        
        processadorExtracao = new ProcessadorExtracaoResultado();
        
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        
        servicoExtracao = new ServicoExtracaoLoteria(
                webClientBuilder, 
                processadorExtracao,
                circuitBreakerRegistry,
                timeLimiterRegistry
        );
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void deveExtrairResultadosComSucesso() {
        // Configurar mock da API da Caixa
        wireMockServer.stubFor(get(urlPathMatching(".*/loterias.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("""
                            <html>
                                <body>
                                    <div>Concurso 2700</div>
                                    <div>01-05-12-23-35-42</div>
                                    <div>15/12/2024</div>
                                    <div>Acumulou!</div>
                                    <div>R$ 50.000.000,00</div>
                                </body>
                            </html>
                            """)));

        StepVerifier.create(servicoExtracao.extrairResultados("megasena", "2024-12-15"))
                .expectNextMatches(resultado -> {
                    System.out.println("Resultado extraído: " + resultado);
                    return resultado.containsKey("modalidade") && 
                           resultado.get("modalidade").equals("megasena") &&
                           resultado.containsKey("dataExtracao");
                })
                .verifyComplete();

        // Verificar que a requisição foi feita
        wireMockServer.verify(getRequestedFor(urlPathMatching(".*/loterias.*")));
    }

    @Test
    void deveRetornarErroQuandoServicoIndisponivel() {
        // Simular erro 500
        wireMockServer.stubFor(get(urlPathMatching(".*/loterias.*"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        StepVerifier.create(servicoExtracao.extrairResultados("megasena", null))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void deveRespeitarTimeoutConfigurado() {
        // Simular timeout
        wireMockServer.stubFor(get(urlPathMatching(".*/loterias.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(35000) // 35 segundos - maior que timeout de 30s
                        .withBody("Delayed response")));

        StepVerifier.create(servicoExtracao.extrairResultados("megasena", null))
                .expectError(RuntimeException.class)
                .verify(Duration.ofSeconds(35)); // Timeout de teste maior
    }

    @Test
    void deveRealizarRetryQuandoErro5xx() {
        // Primeira tentativa: 500, segunda tentativa: 200
        wireMockServer.stubFor(get(urlPathMatching(".*/loterias.*"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error"))
                .willSetStateTo("first-attempt"));

        wireMockServer.stubFor(get(urlPathMatching(".*/loterias.*"))
                .inScenario("retry-scenario")
                .whenScenarioStateIs("first-attempt")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody("<html><body>Concurso 2700</body></html>")));

        StepVerifier.create(servicoExtracao.extrairResultados("megasena", null))
                .expectNextCount(1) // Deve ter sucesso após retry
                .verifyComplete();

        // Verificar que foram feitas 2 requisições
        wireMockServer.verify(2, getRequestedFor(urlPathMatching(".*/loterias.*")));
    }
}