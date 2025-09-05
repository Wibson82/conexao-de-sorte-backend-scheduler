package br.tec.facilitaservicos.scheduler.apresentacao.controlador;

import br.tec.facilitaservicos.scheduler.SchedulerApplication;
import br.tec.facilitaservicos.scheduler.apresentacao.dto.EtlJobRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Testes de integração para JobController.
 */
@SpringBootTest(classes = SchedulerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class JobControllerTest {

    @LocalServerPort
    private int port;

    @Test
    @WithMockUser(authorities = {"SCOPE_scheduler.write"})
    void deveIniciarJobETL() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        EtlJobRequest request = new EtlJobRequest("megasena", "2024-01-01");

        client.post()
                .uri("/jobs/loterias/etl")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jobId").exists();
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_scheduler.read"})
    void deveObterStatusSchedulers() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get()
                .uri("/jobs/diagnostico/schedulers")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.active").exists()
                .jsonPath("$.jobs").exists();
    }

    @Test
    @WithMockUser(authorities = {"SCOPE_scheduler.read"})
    void deveTestarScheduler() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        client.get()
                .uri("/jobs/diagnostico/teste-scheduler")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(response -> {
                    assert response.contains("Scheduler funcionando corretamente");
                });
    }
}