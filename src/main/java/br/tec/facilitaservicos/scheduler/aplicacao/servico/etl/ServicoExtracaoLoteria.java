package br.tec.facilitaservicos.scheduler.aplicacao.servico.etl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Serviço de extração de dados de loterias.
 * Versão reativa baseada no ServicoExtracaoLoteria do backend original.
 */
@Service
public class ServicoExtracaoLoteria {

    private static final Logger logger = LoggerFactory.getLogger(ServicoExtracaoLoteria.class);
    
    private final WebClient webClient;
    private final ProcessadorExtracaoResultado processadorExtracao;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public ServicoExtracaoLoteria(WebClient.Builder webClientBuilder, 
                                 ProcessadorExtracaoResultado processadorExtracao,
                                 CircuitBreakerRegistry circuitBreakerRegistry,
                                 TimeLimiterRegistry timeLimiterRegistry) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.processadorExtracao = processadorExtracao;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("loteria-extraction");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("loteria-extraction");
    }

    /**
     * Extrai resultados de uma modalidade específica com timeout e circuit breaker.
     */
    public Mono<Map<String, Object>> extrairResultados(String modalidade, String data) {
        return Mono.fromCallable(() -> {
            // Configurar MDC para logs estruturados
            MDC.put("modalidade", modalidade);
            MDC.put("data", data != null ? data : "hoje");
            return modalidade;
        })
        .flatMap(m -> realizarExtracaoComRobustez(m, data))
        .doFinally(signalType -> MDC.clear());
    }
    
    private Mono<Map<String, Object>> realizarExtracaoComRobustez(String modalidade, String data) {
        logger.info(Markers.append("operacao", "extracao_iniciada"), 
                   "Iniciando extração de dados");
        
        return construirUrl(modalidade, data)
                .flatMap(this::realizarRequisicaoComTimeout)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .flatMap(html -> processadorExtracao.processar(modalidade, html))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(this::isRetryableError)
                    .doBeforeRetry(retrySignal -> 
                        logger.warn(Markers.append("retryAttempt", retrySignal.totalRetries() + 1), 
                                   "Tentativa de retry na extração", retrySignal.failure())))
                .doOnNext(resultado -> 
                    logger.info(Markers.append("resultadosEncontrados", resultado.size())
                                      .and(Markers.append("operacao", "extracao_concluida")), 
                               "Extração concluída com sucesso"))
                .doOnError(error -> {
                    if (error instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
                        logger.error(Markers.append("circuitBreakerOpen", true), 
                                    "Extração falhou - Circuit Breaker Aberto", error);
                    } else if (error instanceof java.util.concurrent.TimeoutException) {
                        logger.error(Markers.append("timeout", true), 
                                    "Extração falhou - Timeout", error);
                    } else {
                        logger.error(Markers.append("errorType", error.getClass().getSimpleName()), 
                                    "Erro na extração", error);
                    }
                });
    }

    private Mono<String> construirUrl(String modalidade, String data) {
        return Mono.fromCallable(() -> {
            String baseUrl = obterUrlBase(modalidade);
            if (data != null && !data.isEmpty()) {
                LocalDate dataLocal = LocalDate.parse(data, DateTimeFormatter.ISO_LOCAL_DATE);
                return String.format("%s?data=%s", baseUrl, dataLocal.format(DateTimeFormatter.ISO_LOCAL_DATE));
            }
            return baseUrl;
        });
    }

    private Mono<String> realizarRequisicaoComTimeout(String url) {
        logger.debug(Markers.append("url", url), "Realizando requisição HTTP");
        
        return webClient.get()
                .uri(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                .header("Cache-Control", "no-cache")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .doOnNext(html -> 
                    logger.debug(Markers.append("responseSize", html.length()), 
                                "Resposta HTTP recebida com sucesso"))
                .onErrorMap(WebClientRequestException.class, 
                           ex -> new RuntimeException("Erro de conectividade na requisição HTTP para: " + url, ex))
                .onErrorMap(WebClientResponseException.class,
                           ex -> new RuntimeException("Erro HTTP " + ex.getStatusCode() + " para: " + url, ex))
                .onErrorMap(java.util.concurrent.TimeoutException.class,
                           ex -> new RuntimeException("Timeout na requisição HTTP para: " + url, ex));
    }
    
    private boolean isRetryableError(Throwable throwable) {
        return throwable instanceof WebClientRequestException ||
               (throwable instanceof WebClientResponseException && 
                ((WebClientResponseException) throwable).getStatusCode().is5xxServerError()) ||
               (throwable instanceof RuntimeException && 
                throwable.getMessage().contains("Timeout"));
    }

    private String obterUrlBase(String modalidade) {
        // URLs baseadas no serviço original - adaptar conforme necessário
        return switch (modalidade.toLowerCase()) {
            case "megasena" -> "https://loterias.caixa.gov.br/wps/portal/loterias/landing/megasena";
            case "quina" -> "https://loterias.caixa.gov.br/wps/portal/loterias/landing/quina";
            case "lotofacil" -> "https://loterias.caixa.gov.br/wps/portal/loterias/landing/lotofacil";
            case "lotomania" -> "https://loterias.caixa.gov.br/wps/portal/loterias/landing/lotomania";
            default -> throw new IllegalArgumentException("Modalidade não suportada: " + modalidade);
        };
    }
}