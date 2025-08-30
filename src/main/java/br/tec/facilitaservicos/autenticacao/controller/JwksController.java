package br.tec.facilitaservicos.autenticacao.controller;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.tec.facilitaservicos.autenticacao.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 🔑 CONTROLADOR JWKS - ENTERPRISE GRADE WEBFLUX
 * ============================================================================
 * 
 * Controlador reativo para exposição do JWK Set conforme RFC 7517.
 * Implementa:
 * - Endpoint público para JWK Set
 * - Suporte a rotação de chaves
 * - Cache inteligente com headers HTTP apropriados
 * - Headers de segurança adequados
 * - Observabilidade e métricas
 * 
 * Endpoints:
 * - GET /.well-known/jwks.json - JWK Set público (RFC 7517)
 * - GET /oauth2/jwks - Alias para compatibilidade
 * 
 * ============================================================================
 */
@RestController
@RequestMapping
@Tag(name = "JWKS", description = "Endpoints para JWK Set (JSON Web Key Set)")
public class JwksController {

    private static final Logger logger = LoggerFactory.getLogger(JwksController.class);

    // ============================================================================
    // 🔧 CONSTANTES DE CONFIGURAÇÃO - AMBIENTE DE PRODUÇÃO
    // ============================================================================

    // Endpoints
    private static final String JWKS_ENDPOINT = "/.well-known/jwks.json";
    private static final String JWKS_ALTERNATIVE_ENDPOINT = "/oauth2/jwks";
    private static final String KEY_INFO_ENDPOINT = "/.well-known/key-info";
    private static final String HEALTH_CHECK_ENDPOINT = "/oauth2/jwks/health";

    // Cache TTL
    private static final Duration JWKS_CACHE_TTL = Duration.ofHours(1);
    private static final Duration KEY_INFO_CACHE_TTL = Duration.ofMinutes(5);
    private static final int KEY_INFO_CACHE_TTL_SECONDS = 3600;

    // Headers CORS
    private static final String CORS_ALLOW_ORIGIN = "*";
    private static final String CORS_ALLOW_METHODS = "GET, OPTIONS";
    private static final String CORS_ALLOW_HEADERS = "Content-Type";

    // Chaves de resposta JSON
    private static final String JSON_KEY_KEYS = "keys";
    private static final String JSON_KEY_ERROR = "error";
    private static final String JSON_KEY_ERROR_DESCRIPTION = "error_description";
    private static final String JSON_KEY_STATUS = "status";
    private static final String JSON_KEY_SERVICE = "service";
    private static final String JSON_KEY_KEYS_AVAILABLE = "keys_available";
    private static final String JSON_KEY_KEY_COUNT = "key_count";
    private static final String JSON_KEY_TIMESTAMP = "timestamp";

    // Valores de resposta
    private static final String ERROR_INTERNAL_SERVER = "internal_server_error";
    private static final String ERROR_NO_KEY_AVAILABLE = "no_key_available";
    private static final String ERROR_SERVICE_UNAVAILABLE = "service_unavailable";
    private static final String STATUS_UP = "UP";
    private static final String STATUS_DOWN = "DOWN";
    private static final String SERVICE_NAME = "jwks";

    // Valores padrão para chaves JWT
    private static final String DEFAULT_KEY_ID = "unknown";
    private static final String DEFAULT_ALGORITHM = "RS256";
    private static final String DEFAULT_KEY_TYPE = "RSA";
    private static final String DEFAULT_KEY_USE = "sig";

    // Chaves JWK
    private static final String JWK_KEY_ID = "kid";
    private static final String JWK_ALGORITHM = "alg";
    private static final String JWK_KEY_TYPE = "kty";
    private static final String JWK_KEY_USE = "use";

    // Chaves de informação da chave
    private static final String KEY_INFO_KEY_ID = "key_id";
    private static final String KEY_INFO_ALGORITHM = "algorithm";
    private static final String KEY_INFO_KEY_TYPE = "key_type";
    private static final String KEY_INFO_KEY_USE = "key_use";
    private static final String KEY_INFO_ISSUED_AT = "issued_at";
    private static final String KEY_INFO_CACHE_TTL_KEY = "cache_ttl_seconds";

    private final JwtService jwtService;

    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * 🛡️ Método helper para criar respostas de erro padronizadas.
     * Implementa programação defensiva para ambiente de produção.
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(int status, String error, String description) {
        if (error == null || error.trim().isEmpty()) {
            error = ERROR_INTERNAL_SERVER;
        }
        if (description == null || description.trim().isEmpty()) {
            description = "Erro interno do servidor";
        }

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put(JSON_KEY_ERROR, error);
        errorBody.put(JSON_KEY_ERROR_DESCRIPTION, description);

        return ResponseEntity.status(status)
            .cacheControl(CacheControl.noCache())
            .body(errorBody);
    }
    
    /**
     * 🔑 Endpoint principal do JWK Set (RFC 7517).
     * 
     * Este é o endpoint padrão para descoberta de chaves públicas usado por
     * clientes OAuth2/OpenID Connect para validar tokens JWT.
     */
    @GetMapping(value = JWKS_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Obter JWK Set",
        description = "Retorna o JSON Web Key Set (JWKS) conforme RFC 7517 para validação de tokens JWT"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "JWK Set retornado com sucesso",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(
                    description = "JWK Set conforme RFC 7517",
                    example = """
                        {
                          "keys": [
                            {
                              "kty": "RSA",
                              "use": "sig",
                              "alg": "RS256",
                              "kid": "key-id-123",
                              "n": "base64url-encoded-modulus",
                              "e": "AQAB"
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erro interno do servidor ao gerar JWK Set"
        )
    })
    public Mono<ResponseEntity<Map<String, Object>>> getJwkSet() {
        logger.debug("🔑 Solicitação de JWK Set recebida");

        // Programação defensiva: validação do serviço
        if (jwtService == null) {
            logger.error("❌ JwtService não está disponível");
            return Mono.just(createErrorResponse(500, ERROR_SERVICE_UNAVAILABLE, "Serviço JWT não disponível"));
        }

        return jwtService.generateJwkSet()
            .map(jwkSet -> {
                // Programação defensiva: validação do JWK Set
                if (jwkSet == null || jwkSet.isEmpty()) {
                    logger.warn("⚠️ JWK Set vazio ou nulo gerado");
                    return createErrorResponse(500, ERROR_NO_KEY_AVAILABLE, "Nenhuma chave disponível");
                }

                logger.debug("✅ JWK Set gerado com sucesso");

                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(JWKS_CACHE_TTL).cachePublic())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Access-Control-Allow-Origin", CORS_ALLOW_ORIGIN)
                    .header("Access-Control-Allow-Methods", CORS_ALLOW_METHODS)
                    .header("Access-Control-Allow-Headers", CORS_ALLOW_HEADERS)
                    .body(jwkSet);
            })
            .doOnSuccess(response -> {
                if (response != null && response.getStatusCode().is2xxSuccessful()) {
                    logger.info("🔑 JWK Set servido com sucesso - Cache: {}h", JWKS_CACHE_TTL.toHours());
                }
            })
            .doOnError(error ->
                logger.error("❌ Erro ao gerar JWK Set: {}",
                    error != null ? error.getMessage() : "Erro desconhecido", error)
            )
            .onErrorReturn(createErrorResponse(500, ERROR_INTERNAL_SERVER, "Erro interno ao gerar JWK Set"));
    }
    
    /**
     * 🔑 Endpoint alternativo para compatibilidade.
     */
    @GetMapping(value = JWKS_ALTERNATIVE_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Obter JWK Set (endpoint alternativo)",
        description = "Endpoint alternativo para compatibilidade - redireciona para " + JWKS_ENDPOINT
    )
    @ApiResponse(responseCode = "200", description = "JWK Set retornado com sucesso")
    public Mono<ResponseEntity<Map<String, Object>>> getJwkSetAlternative() {
        logger.debug("🔑 Solicitação de JWK Set via endpoint alternativo");
        return getJwkSet();
    }
    
    /**
     * 📊 Endpoint de informações da chave atual.
     * Útil para monitoramento e debug.
     */
    @GetMapping(value = KEY_INFO_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Informações da chave de assinatura",
        description = "Retorna informações sobre a chave de assinatura atual (sem dados sensíveis)"
    )
    @ApiResponse(responseCode = "200", description = "Informações da chave retornadas com sucesso")
    public Mono<ResponseEntity<Map<String, Object>>> getKeyInfo() {
        logger.debug("📊 Solicitação de informações da chave");

        // Programação defensiva: validação do serviço
        if (jwtService == null) {
            logger.error("❌ JwtService não está disponível para informações da chave");
            return Mono.just(createErrorResponse(500, ERROR_SERVICE_UNAVAILABLE, "Serviço JWT não disponível"));
        }

        return jwtService.generateJwkSet()
            .map(jwkSet -> {
                // Programação defensiva: validação do JWK Set
                if (jwkSet == null || jwkSet.isEmpty()) {
                    logger.warn("⚠️ JWK Set vazio para informações da chave");
                    Map<String, Object> errorBody = new HashMap<>();
                    errorBody.put(JSON_KEY_ERROR, ERROR_NO_KEY_AVAILABLE);
                    return ResponseEntity.ok().body(errorBody);
                }

                // Extrair informações da primeira (e única) chave
                @SuppressWarnings("unchecked")
                var keys = (java.util.List<Map<String, Object>>) jwkSet.get(JSON_KEY_KEYS);

                Map<String, Object> responseBody = new HashMap<>();

                if (keys != null && !keys.isEmpty()) {
                    Map<String, Object> key = keys.get(0);

                    // Programação defensiva: validação da chave
                    if (key == null) {
                        logger.warn("⚠️ Chave nula encontrada no JWK Set");
                        responseBody.put(JSON_KEY_ERROR, ERROR_NO_KEY_AVAILABLE);
                    } else {
                        responseBody.put(KEY_INFO_KEY_ID, key.getOrDefault(JWK_KEY_ID, DEFAULT_KEY_ID));
                        responseBody.put(KEY_INFO_ALGORITHM, key.getOrDefault(JWK_ALGORITHM, DEFAULT_ALGORITHM));
                        responseBody.put(KEY_INFO_KEY_TYPE, key.getOrDefault(JWK_KEY_TYPE, DEFAULT_KEY_TYPE));
                        responseBody.put(KEY_INFO_KEY_USE, key.getOrDefault(JWK_KEY_USE, DEFAULT_KEY_USE));
                        responseBody.put(KEY_INFO_ISSUED_AT, System.currentTimeMillis() / 1000);
                        responseBody.put(KEY_INFO_CACHE_TTL_KEY, KEY_INFO_CACHE_TTL_SECONDS);

                        logger.debug("📊 Informações da chave geradas: kid={}", key.get(JWK_KEY_ID));
                    }
                } else {
                    logger.warn("⚠️ Nenhuma chave encontrada no JWK Set");
                    responseBody.put(JSON_KEY_ERROR, ERROR_NO_KEY_AVAILABLE);
                }

                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(KEY_INFO_CACHE_TTL))
                    .body(responseBody);
            })
            .doOnError(error ->
                logger.error("❌ Erro ao obter informações da chave: {}",
                    error != null ? error.getMessage() : "Erro desconhecido")
            )
            .onErrorReturn(createErrorResponse(500, ERROR_INTERNAL_SERVER, "Erro interno ao obter informações da chave"));
    }
    
    /**
     * ✨ Endpoint de health check específico para JWKS.
     */
    @GetMapping(value = HEALTH_CHECK_ENDPOINT, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Health check do serviço JWKS",
        description = "Verifica se o serviço JWKS está funcionando corretamente"
    )
    @ApiResponse(responseCode = "200", description = "Serviço JWKS está saudável")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        logger.debug("✨ Health check JWKS solicitado");

        // Programação defensiva: validação do serviço
        if (jwtService == null) {
            logger.error("❌ JwtService não está disponível para health check");
            Map<String, Object> errorHealth = new HashMap<>();
            errorHealth.put(JSON_KEY_STATUS, STATUS_DOWN);
            errorHealth.put(JSON_KEY_SERVICE, SERVICE_NAME);
            errorHealth.put(JSON_KEY_ERROR, ERROR_SERVICE_UNAVAILABLE);
            errorHealth.put(JSON_KEY_TIMESTAMP, System.currentTimeMillis());

            return Mono.just(ResponseEntity.status(500)
                .cacheControl(CacheControl.noCache())
                .body(errorHealth));
        }

        return jwtService.generateJwkSet()
            .map(jwkSet -> {
                // Programação defensiva: validação do JWK Set
                @SuppressWarnings("unchecked")
                var keys = jwkSet != null ?
                    (java.util.List<Map<String, Object>>) jwkSet.get(JSON_KEY_KEYS) : null;
                boolean hasKeys = keys != null && !keys.isEmpty();

                Map<String, Object> health = new HashMap<>();
                health.put(JSON_KEY_STATUS, hasKeys ? STATUS_UP : STATUS_DOWN);
                health.put(JSON_KEY_SERVICE, SERVICE_NAME);
                health.put(JSON_KEY_KEYS_AVAILABLE, hasKeys);
                health.put(JSON_KEY_KEY_COUNT, hasKeys ? keys.size() : 0);
                health.put(JSON_KEY_TIMESTAMP, System.currentTimeMillis());

                return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(health);
            })
            .doOnSuccess(response -> {
                if (response != null) {
                    logger.debug("✨ Health check JWKS concluído - Status: {}",
                        response.getStatusCode().is2xxSuccessful() ? "OK" : "ERRO");
                }
            })
            .doOnError(error ->
                logger.warn("⚠️ Health check JWKS falhou: {}",
                    error != null ? error.getMessage() : "Erro desconhecido")
            )
            .onErrorReturn(createErrorResponse(500, ERROR_SERVICE_UNAVAILABLE, "Serviço JWKS indisponível"));
    }
}