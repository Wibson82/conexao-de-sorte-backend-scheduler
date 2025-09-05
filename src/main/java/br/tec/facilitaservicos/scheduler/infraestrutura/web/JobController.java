package br.tec.facilitaservicos.scheduler.infraestrutura.web;

import br.tec.facilitaservicos.scheduler.aplicacao.servico.JobService;
import br.tec.facilitaservicos.scheduler.aplicacao.dto.JobDto;
import br.tec.facilitaservicos.scheduler.aplicacao.dto.CriarJobRequest;
import br.tec.facilitaservicos.scheduler.aplicacao.dto.AtualizarJobRequest;
import br.tec.facilitaservicos.scheduler.aplicacao.dto.EstatisticasJobDto;
import br.tec.facilitaservicos.scheduler.dominio.enums.StatusJob;
import br.tec.facilitaservicos.scheduler.dominio.enums.TipoJob;
import br.tec.facilitaservicos.scheduler.dominio.enums.PrioridadeJob;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ============================================================================
 * 🎮 CONTROLLER REST PARA GERENCIAMENTO DE JOBS
 * ============================================================================
 * 
 * Controller reativo para operações CRUD e monitoramento de jobs.
 * Fornece APIs RESTful para:
 * 
 * 📋 OPERAÇÕES BÁSICAS:
 * - Criar, listar, atualizar e remover jobs
 * - Buscar jobs por diversos filtros
 * - Paginação e ordenação
 * 
 * ⚡ OPERAÇÕES DE EXECUÇÃO:
 * - Agendar jobs
 * - Executar jobs imediatamente
 * - Cancelar execuções
 * - Pausar e retomar
 * 
 * 📊 MONITORAMENTO:
 * - Estatísticas detalhadas
 * - Métricas por tipo
 * - Health checks
 * - Status de circuit breaker
 * 
 * 🔧 MANUTENÇÃO:
 * - Limpeza de jobs antigos
 * - Reset de circuit breaker
 * - Arquivamento automático
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping("/rest/v1/jobs")
@Tag(name = "Jobs", description = "Gerenciamento de jobs do scheduler")
@SecurityRequirement(name = "bearerAuth")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);
    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    // === OPERAÇÕES CRUD ===

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_scheduler_write') or hasAuthority('SCOPE_admin')")
    @Operation(summary = "Criar novo job", description = "Cria um novo job no scheduler")
    @ApiResponse(responseCode = "201", description = "Job criado com sucesso")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    public Mono<ResponseEntity<JobDto>> criarJob(
            @Valid @RequestBody CriarJobRequest request,
            Authentication authentication) {
        
        String userId = authentication.getName();
        log.info("🆕 Usuário {} solicitou criação de job: {}", userId, request.getNome());
        
        return jobService.criarJob(request)
            .map(job -> ResponseEntity.status(HttpStatus.CREATED).body(job))
            .doOnSuccess(response -> log.info("✅ Job criado com sucesso: {}", 
                response.getBody().getNome()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar job por ID", description = "Obtém detalhes de um job específico")
    @ApiResponse(responseCode = "200", description = "Job encontrado")
    @ApiResponse(responseCode = "404", description = "Job não encontrado")
    public Mono<ResponseEntity<JobDto>> buscarJobPorId(@PathVariable String id) {
        return jobService.buscarJobPorId(id)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.debug("🔍 Job encontrado: {}", id));
    }

    @GetMapping
    @Operation(summary = "Listar jobs", description = "Lista jobs com filtros opcionais")
    @ApiResponse(responseCode = "200", description = "Lista de jobs")
    public Flux<JobDto> listarJobs(
            @Parameter(description = "Filtrar por nome") @RequestParam(required = false) String nome,
            @Parameter(description = "Filtrar por grupo") @RequestParam(required = false) String grupo,
            @Parameter(description = "Filtrar por tipo") @RequestParam(required = false) TipoJob tipo,
            @Parameter(description = "Filtrar por status") @RequestParam(required = false) StatusJob status,
            @Parameter(description = "Filtrar por prioridade") @RequestParam(required = false) PrioridadeJob prioridade,
            @Parameter(description = "Data início") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @Parameter(description = "Ordenação") @RequestParam(defaultValue = "data") String ordenacao,
            @Parameter(description = "Limite") @RequestParam(defaultValue = "50") int limite,
            @Parameter(description = "Offset") @RequestParam(defaultValue = "0") int offset) {
        
        return jobService.listarJobs(nome, grupo, tipo, status, prioridade, 
            dataInicio, ordenacao, limite, offset)
            .doOnSubscribe(s -> log.debug("📋 Listando jobs com filtros aplicados"));
    }

    @GetMapping("/count")
    @Operation(summary = "Contar jobs", description = "Conta jobs com filtros aplicados")
    @ApiResponse(responseCode = "200", description = "Contagem de jobs")
    public Mono<ResponseEntity<Map<String, Long>>> contarJobs(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String grupo,
            @RequestParam(required = false) TipoJob tipo,
            @RequestParam(required = false) StatusJob status,
            @RequestParam(required = false) PrioridadeJob prioridade,
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio) {
        
        return jobService.contarJobs(nome, grupo, tipo, status, prioridade, dataInicio)
            .map(count -> ResponseEntity.ok(Map.of("total", count)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar job", description = "Atualiza configurações de um job")
    @ApiResponse(responseCode = "200", description = "Job atualizado com sucesso")
    @ApiResponse(responseCode = "404", description = "Job não encontrado")
    @ApiResponse(responseCode = "400", description = "Job não pode ser editado")
    public Mono<ResponseEntity<JobDto>> atualizarJob(
            @PathVariable String id, 
            @Valid @RequestBody AtualizarJobRequest request) {
        
        log.info("📝 Atualizando job: {}", id);
        
        return jobService.atualizarJob(id, request)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Job atualizado: {}", id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remover job", description = "Remove um job do scheduler")
    @ApiResponse(responseCode = "204", description = "Job removido com sucesso")
    @ApiResponse(responseCode = "404", description = "Job não encontrado")
    @ApiResponse(responseCode = "400", description = "Job não pode ser removido")
    public Mono<ResponseEntity<Void>> removerJob(@PathVariable String id) {
        log.info("🗑️ Removendo job: {}", id);
        
        return jobService.removerJob(id)
            .then(Mono.just(ResponseEntity.noContent().<Void>build()))
            .doOnSuccess(response -> log.info("✅ Job removido: {}", id));
    }

    // === OPERAÇÕES DE EXECUÇÃO ===

    @PostMapping("/{id}/agendar")
    @Operation(summary = "Agendar job", description = "Agenda job para execução")
    @ApiResponse(responseCode = "200", description = "Job agendado com sucesso")
    @ApiResponse(responseCode = "404", description = "Job não encontrado")
    @ApiResponse(responseCode = "400", description = "Job não pode ser agendado")
    public Mono<ResponseEntity<JobDto>> agendarJob(@PathVariable String id) {
        log.info("📅 Agendando job: {}", id);
        
        return jobService.agendarJob(id)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Job agendado: {}", id));
    }

    @PostMapping("/{id}/executar")
    @Operation(summary = "Executar job", description = "Executa job imediatamente")
    @ApiResponse(responseCode = "200", description = "Job executado com sucesso")
    @ApiResponse(responseCode = "404", description = "Job não encontrado")
    @ApiResponse(responseCode = "400", description = "Job não pode ser executado")
    public Mono<ResponseEntity<JobDto>> executarJob(@PathVariable String id) {
        log.info("▶️ Executando job: {}", id);
        
        return jobService.executarJob(id)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Job executado: {}", id));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar job", description = "Cancela execução de um job")
    @ApiResponse(responseCode = "200", description = "Job cancelado com sucesso")
    @ApiResponse(responseCode = "404", description = "Job não encontrado")
    @ApiResponse(responseCode = "400", description = "Job não pode ser cancelado")
    public Mono<ResponseEntity<JobDto>> cancelarJob(@PathVariable String id) {
        log.info("⏹️ Cancelando job: {}", id);
        
        return jobService.cancelarJob(id)
            .map(ResponseEntity::ok)
            .doOnSuccess(response -> log.info("✅ Job cancelado: {}", id));
    }

    // === OPERAÇÕES DE MONITORAMENTO ===

    @GetMapping("/prontos")
    @Operation(summary = "Jobs prontos para execução", description = "Lista jobs prontos para executar")
    @ApiResponse(responseCode = "200", description = "Lista de jobs prontos")
    public Flux<JobDto> buscarJobsProntosParaExecucao(
            @Parameter(description = "Limite") @RequestParam(defaultValue = "20") int limite) {
        
        return jobService.buscarJobsProntosParaExecucao(limite)
            .doOnSubscribe(s -> log.debug("🔍 Buscando jobs prontos para execução"));
    }

    @GetMapping("/timeout")
    @Operation(summary = "Jobs com timeout", description = "Lista jobs que excederam o timeout")
    @ApiResponse(responseCode = "200", description = "Lista de jobs com timeout")
    public Flux<JobDto> buscarJobsComTimeout() {
        return jobService.buscarJobsComTimeout()
            .doOnSubscribe(s -> log.debug("⏱️ Buscando jobs com timeout"));
    }

    @GetMapping("/retry")
    @Operation(summary = "Jobs para retry", description = "Lista jobs prontos para retry")
    @ApiResponse(responseCode = "200", description = "Lista de jobs para retry")
    public Flux<JobDto> buscarJobsParaRetry(
            @Parameter(description = "Limite") @RequestParam(defaultValue = "10") int limite) {
        
        return jobService.buscarJobsParaRetry(limite)
            .doOnSubscribe(s -> log.debug("🔄 Buscando jobs para retry"));
    }

    @GetMapping("/estatisticas")
    @Operation(summary = "Estatísticas gerais", description = "Obtém estatísticas gerais dos jobs")
    @ApiResponse(responseCode = "200", description = "Estatísticas dos jobs")
    public Mono<ResponseEntity<EstatisticasJobDto>> obterEstatisticasGerais(
            @Parameter(description = "Data início") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio) {
        
        LocalDateTime inicio = dataInicio != null ? dataInicio : LocalDateTime.now().minusDays(7);
        
        return jobService.obterEstatisticasGerais(inicio)
            .map(ResponseEntity::ok)
            .doOnSubscribe(s -> log.debug("📊 Obtendo estatísticas gerais"));
    }

    @GetMapping("/estatisticas/tipo")
    @Operation(summary = "Estatísticas por tipo", description = "Obtém estatísticas agrupadas por tipo")
    @ApiResponse(responseCode = "200", description = "Estatísticas por tipo")
    public Flux<Map<String, Object>> obterEstatisticasPorTipo(
            @Parameter(description = "Data início") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio) {
        
        LocalDateTime inicio = dataInicio != null ? dataInicio : LocalDateTime.now().minusDays(7);
        
        return jobService.obterEstatisticasPorTipo(inicio)
            .doOnSubscribe(s -> log.debug("📊 Obtendo estatísticas por tipo"));
    }

    // === OPERAÇÕES DE MANUTENÇÃO ===

    @PostMapping("/circuit-breaker/reset")
    @Operation(summary = "Reset circuit breaker", description = "Reseta circuit breaker dos jobs")
    @ApiResponse(responseCode = "200", description = "Circuit breaker resetado")
    public Flux<JobDto> resetCircuitBreaker() {
        log.info("🔧 Resetando circuit breaker");
        
        return jobService.resetCircuitBreaker()
            .doOnComplete(() -> log.info("✅ Circuit breaker resetado"));
    }

    @DeleteMapping("/limpeza/completados")
    @Operation(summary = "Remover jobs completados antigos", description = "Remove jobs completados há mais de X dias")
    @ApiResponse(responseCode = "200", description = "Jobs removidos")
    public Mono<ResponseEntity<Map<String, Integer>>> removerJobsAntigosCompletados(
            @Parameter(description = "Dias") @RequestParam(defaultValue = "30") int dias) {
        
        log.info("🧹 Removendo jobs completados antigos (>{} dias)", dias);
        
        return jobService.removerJobsAntigosCompletados(java.time.Duration.ofDays(dias))
            .map(count -> ResponseEntity.ok(Map.of("removidos", count)))
            .doOnSuccess(response -> log.info("✅ {} jobs antigos removidos", 
                response.getBody().get("removidos")));
    }

    @PostMapping("/arquivar/antigos")
    @Operation(summary = "Arquivar jobs antigos", description = "Arquiva jobs finalizados há mais de X dias")
    @ApiResponse(responseCode = "200", description = "Jobs arquivados")
    public Mono<ResponseEntity<Map<String, Integer>>> arquivarJobsAntigos(
            @Parameter(description = "Dias") @RequestParam(defaultValue = "90") int dias) {
        
        log.info("📦 Arquivando jobs antigos (>{} dias)", dias);
        
        return jobService.arquivarJobsAntigos(java.time.Duration.ofDays(dias))
            .map(count -> ResponseEntity.ok(Map.of("arquivados", count)))
            .doOnSuccess(response -> log.info("✅ {} jobs arquivados", 
                response.getBody().get("arquivados")));
    }

    // === HEALTH CHECK ===

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica saúde do sistema de jobs")
    @ApiResponse(responseCode = "200", description = "Sistema saudável")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        return jobService.contarJobs(null, null, null, null, null, LocalDateTime.now().minusHours(1))
            .map(count -> {
                Map<String, Object> health = Map.of(
                    "status", "UP",
                    "timestamp", LocalDateTime.now(),
                    "jobsUltimaHora", count
                );
                return ResponseEntity.ok(health);
            })
            .onErrorReturn(ResponseEntity.ok(Map.of(
                "status", "DOWN",
                "timestamp", LocalDateTime.now(),
                "error", "Erro ao verificar jobs"
            )));
    }
}