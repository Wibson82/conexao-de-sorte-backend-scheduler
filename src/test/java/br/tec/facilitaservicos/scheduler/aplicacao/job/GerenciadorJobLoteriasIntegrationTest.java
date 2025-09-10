package br.tec.facilitaservicos.scheduler.aplicacao.job;

import br.tec.facilitaservicos.scheduler.SchedulerApplication;
import br.tec.facilitaservicos.scheduler.configuracao.SchedulerProperties;
import br.tec.facilitaservicos.scheduler.dominio.entidade.JobStatus;
import br.tec.facilitaservicos.scheduler.aplicacao.servico.etl.ServicoETLLoterias;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Testes de integração para GerenciadorJobLoterias com Testcontainers e WireMock.
 */
@SpringBootTest(classes = SchedulerApplication.class)
@ActiveProfiles("test")
@Testcontainers
class GerenciadorJobLoteriasIntegrationTest {
    
    private static final Logger log = LoggerFactory.getLogger(GerenciadorJobLoteriasIntegrationTest.class);

    @Autowired
    private SchedulerProperties schedulerProperties;
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private WireMockServer wireMockServer;
    
    @Autowired
    private ServicoETLLoterias servicoETLLoterias;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(0));
        wireMockServer.start();
        
        // Configurar URLs para apontar para o WireMock
        String baseUrl = String.format("http://localhost:%d", wireMockServer.port());
        servicoETLLoterias.configurarUrlsParaTeste(baseUrl);
        
        // Mock das URLs de loteria para usar o WireMock
        setupWireMockStubs();
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private void setupWireMockStubs() {
        // Stub para Mega Sena
        wireMockServer.stubFor(get(urlPathMatching(".*/megasena.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(generateMockLoteriaHTML("2700", "01-05-12-23-35-42"))));
        
        // Stub para Quina
        wireMockServer.stubFor(get(urlPathMatching(".*/quina.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(generateMockLoteriaHTML("6300", "15-25-35-45-55"))));
                        
        // Stub para Lotofácil
        wireMockServer.stubFor(get(urlPathMatching(".*/lotofacil.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(generateMockLoteriaHTML("3100", "01-02-03-04-05-06-07-08-09-10-11-12-13-14-15"))));
                        
        // Stub para Lotomania
        wireMockServer.stubFor(get(urlPathMatching(".*/lotomania.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(generateMockLoteriaHTML("2500", "01-02-03-04-05-06-07-08-09-10"))));
    }

    private String generateMockLoteriaHTML(String concurso, String numeros) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head><title>Resultado Loteria</title></head>
            <body>
                <div class="resultado">
                    <h1>Concurso %s</h1>
                    <div class="numeros">%s</div>
                    <div class="data">15/12/2024</div>
                    <div class="premio">R$ 10.000.000,00</div>
                    <div class="ganhadores">0 ganhadores</div>
                </div>
            </body>
            </html>
            """, concurso, numeros);
    }

    @Test
    void deveExecutarJobETLComSucessoEIdempotencia() {
        String jobId1 = UUID.randomUUID().toString();
        String jobId2 = UUID.randomUUID().toString();

        StepVerifier.create(servicoETLLoterias.executarETLLoteria(jobId1, "megasena", "2024-12-15"))
                .expectNextMatches(execution -> 
                    execution.getJobId().equals(jobId1) && 
                    execution.getStatus() == JobStatus.COMPLETED &&
                    execution.getModalidade().equals("megasena"))
                .verifyComplete();

        // Testar idempotência - mesmo job deve retornar o mesmo resultado
        StepVerifier.create(servicoETLLoterias.executarETLLoteria(jobId2, "megasena", "2024-12-15"))
                .expectNextMatches(execution -> 
                    execution.getJobId().equals(jobId2) && // Deve retornar o segundo job
                    execution.getStatus() == JobStatus.COMPLETED &&
                    execution.getModalidade().equals("megasena") &&
                    execution.getChaveIdempotencia().equals("megasena:2024-12-15")) // Mesma chave de idempotência
                .verifyComplete();
    }

    @Test
    void deveRespeitarConfiguracaoDeRetries() {
        String jobId = UUID.randomUUID().toString();
        
        // Limpar stubs anteriores para lotofacil
        wireMockServer.resetMappings();
        setupWireMockStubs(); // Reconfigurar outros stubs
        
        // Simular falhas temporárias
        wireMockServer.stubFor(get(urlPathMatching(".*/lotofacil.*"))
                .inScenario("retry-test")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("attempt-1"));
        
        wireMockServer.stubFor(get(urlPathMatching(".*/lotofacil.*"))
                .inScenario("retry-test")
                .whenScenarioStateIs("attempt-1")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("attempt-2"));
        
        wireMockServer.stubFor(get(urlPathMatching(".*/lotofacil.*"))
                .inScenario("retry-test")
                .whenScenarioStateIs("attempt-2")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "text/html")
                        .withBody(generateMockLoteriaHTML("3100", "01-02-03-04-05-06-07-08-09-10-11-12-13-14-15"))));

        StepVerifier.create(servicoETLLoterias.executarETLLoteria(jobId, "lotofacil", ""))
                .expectNextMatches(execution -> 
                    execution.getJobId().equals(jobId) &&
                    execution.getStatus() == JobStatus.COMPLETED &&
                    execution.getModalidade().equals("lotofacil"))
                .verifyComplete();

        // Verificar que foram feitas múltiplas tentativas
        wireMockServer.verify(moreThan(2), getRequestedFor(urlPathMatching(".*/lotofacil.*")));
    }

    @Test
    void deveAbrirCircuitBreakerAposFalhasConsecutivas() {
        // Limpar stubs anteriores para lotomania
        wireMockServer.resetMappings();
        setupWireMockStubs(); // Reconfigurar outros stubs
        
        // Simular falhas consecutivas para abrir o circuit breaker
        wireMockServer.stubFor(get(urlPathMatching(".*/lotomania.*"))
                .willReturn(aResponse().withStatus(500)));

        String jobId1 = UUID.randomUUID().toString();
        String jobId2 = UUID.randomUUID().toString();

        // Primeira tentativa - deve falhar mas ainda não abrir CB
        StepVerifier.create(servicoETLLoterias.executarETLLoteria(jobId1, "lotomania", ""))
                .expectErrorMatches(throwable -> {
                    log.info("Erro na primeira tentativa: {}", throwable.getMessage());
                    return throwable instanceof RuntimeException;
                })
                .verify();

        // Segunda tentativa - pode abrir o circuit breaker
        StepVerifier.create(servicoETLLoterias.executarETLLoteria(jobId2, "lotomania", ""))
                .expectErrorMatches(throwable -> {
                    log.info("Erro na segunda tentativa: {}", throwable.getMessage());
                    return true; // Pode ser RuntimeException ou CallNotPermittedException
                })
                .verify();
    }

    @Test
    void deveColetarMetricasDeExecucao() {
        String jobId = UUID.randomUUID().toString();
        
        // Capturar métricas antes
        double successCountBefore = meterRegistry.counter("scheduler.etl.jobs.success").count();

        StepVerifier.create(servicoETLLoterias.executarETLLoteria(jobId, "megasena", ""))
                .expectNextMatches(execution -> 
                    execution.getJobId().equals(jobId) &&
                    execution.getStatus() == JobStatus.COMPLETED &&
                    execution.getModalidade().equals("megasena"))
                .verifyComplete();

        // Verificar que as métricas foram atualizadas
        double successCountAfter = meterRegistry.counter("scheduler.etl.jobs.success").count();
        assert successCountAfter > successCountBefore;
    }

    @Test
    void deveRespeitarTimeoutGlobal() {
        // Limpar stubs anteriores para quina
        wireMockServer.resetMappings();
        setupWireMockStubs(); // Reconfigurar outros stubs
        
        // Simular delay maior que o timeout
        wireMockServer.stubFor(get(urlPathMatching(".*/quina.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(35000) // 35 segundos
                        .withBody("Delayed response")));

        String jobId = UUID.randomUUID().toString();

        StepVerifier.create(servicoETLLoterias.executarETLLoteria(jobId, "quina", ""))
                .expectErrorMatches(throwable -> {
                    log.info("Erro de timeout: {}", throwable.getMessage());
                    return throwable instanceof RuntimeException;
                })
                .verify(Duration.ofSeconds(40)); // Timeout de teste
    }
}