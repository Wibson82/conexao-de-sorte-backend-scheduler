package br.tec.facilitaservicos.scheduler.infraestrutura.web;

import br.tec.facilitaservicos.scheduler.aplicacao.excecao.JobNaoEncontradoException;
import br.tec.facilitaservicos.scheduler.aplicacao.excecao.JobNaoPodeSerExecutadoException;
import br.tec.facilitaservicos.scheduler.aplicacao.excecao.JobCircuitBreakerAbertoException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * 🚨 TRATADOR GLOBAL DE EXCEÇÕES
 * ============================================================================
 * 
 * Manipulador global para tratar exceções de forma centralizada.
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(JobNaoEncontradoException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleJobNaoEncontrado(
            JobNaoEncontradoException ex, ServerWebExchange exchange) {
        
        log.warn("❌ Job não encontrado: {}", ex.getMessage());
        
        Map<String, Object> error = Map.of(
            "error", "Job não encontrado",
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now(),
            "path", exchange.getRequest().getPath().value()
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(error));
    }

    @ExceptionHandler(JobNaoPodeSerExecutadoException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleJobNaoPodeSerExecutado(
            JobNaoPodeSerExecutadoException ex, ServerWebExchange exchange) {
        
        log.warn("⚠️ Job não pode ser executado: {}", ex.getMessage());
        
        Map<String, Object> error = Map.of(
            "error", "Job não pode ser executado",
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now(),
            "path", exchange.getRequest().getPath().value()
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    @ExceptionHandler(JobCircuitBreakerAbertoException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleCircuitBreakerAberto(
            JobCircuitBreakerAbertoException ex, ServerWebExchange exchange) {
        
        log.warn("🚨 Circuit breaker aberto: {}", ex.getMessage());
        
        Map<String, Object> error = Map.of(
            "error", "Circuit breaker aberto",
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now(),
            "path", exchange.getRequest().getPath().value()
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error));
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationErrors(
            WebExchangeBindException ex, ServerWebExchange exchange) {
        
        log.warn("📋 Erro de validação: {}", ex.getMessage());
        
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (existing, replacement) -> existing
            ));
        
        Map<String, Object> error = Map.of(
            "error", "Dados inválidos",
            "message", "Erro de validação nos campos",
            "fields", fieldErrors,
            "timestamp", LocalDateTime.now(),
            "path", exchange.getRequest().getPath().value()
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericError(
            Exception ex, ServerWebExchange exchange) {
        
        log.error("💥 Erro interno do servidor: {}", ex.getMessage(), ex);
        
        Map<String, Object> error = Map.of(
            "error", "Erro interno do servidor",
            "message", "Ocorreu um erro inesperado",
            "timestamp", LocalDateTime.now(),
            "path", exchange.getRequest().getPath().value()
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error));
    }
}
