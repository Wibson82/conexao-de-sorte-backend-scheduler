package br.tec.facilitaservicos.autenticacao.controller;

import br.tec.facilitaservicos.autenticacao.service.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

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
    
    private final JwtService jwtService;
    
    public JwksController(JwtService jwtService) {
        this.jwtService = jwtService;
    }
    
    /**
     * 🔑 Endpoint principal do JWK Set (RFC 7517).
     * 
     * Este é o endpoint padrão para descoberta de chaves públicas usado por
     * clientes OAuth2/OpenID Connect para validar tokens JWT.
     */
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
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
        
        return jwtService.generateJwkSet()
            .map(jwkSet -> {
                logger.debug("✅ JWK Set gerado com sucesso");
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("Access-Control-Allow-Origin", "*")
                    .header("Access-Control-Allow-Methods", "GET, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Content-Type")
                    .body(jwkSet);
            })
            .doOnSuccess(response -> 
                logger.info("🔑 JWK Set servido com sucesso - Cache: 1h")
            )
            .doOnError(error -> 
                logger.error("❌ Erro ao gerar JWK Set: {}", error.getMessage(), error)
            )
            .onErrorReturn(
                ResponseEntity.status(500)
                    .cacheControl(CacheControl.noCache())
                    .body(Map.of(
                        "error", "internal_server_error",
                        "error_description", "Erro interno ao gerar JWK Set"
                    ))
            );
    }
    
    /**
     * 🔑 Endpoint alternativo para compatibilidade.
     */
    @GetMapping(value = "/oauth2/jwks", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Obter JWK Set (endpoint alternativo)",
        description = "Endpoint alternativo para compatibilidade - redireciona para /.well-known/jwks.json"
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
    @GetMapping(value = "/.well-known/key-info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Informações da chave de assinatura",
        description = "Retorna informações sobre a chave de assinatura atual (sem dados sensíveis)"
    )
    @ApiResponse(responseCode = "200", description = "Informações da chave retornadas com sucesso")
    public Mono<ResponseEntity<Map<String, Object>>> getKeyInfo() {
        logger.debug("📊 Solicitação de informações da chave");
        
        return jwtService.generateJwkSet()
            .map(jwkSet -> {
                // Extrair informações da primeira (e única) chave
                @SuppressWarnings("unchecked")
                var keys = (java.util.List<Map<String, Object>>) jwkSet.get("keys");
                
                if (keys != null && !keys.isEmpty()) {
                    Map<String, Object> key = keys.get(0);
                    
                    Map<String, Object> keyInfo = Map.of(
                        "key_id", key.getOrDefault("kid", "unknown"),
                        "algorithm", key.getOrDefault("alg", "RS256"),
                        "key_type", key.getOrDefault("kty", "RSA"),
                        "key_use", key.getOrDefault("use", "sig"),
                        "issued_at", System.currentTimeMillis() / 1000,
                        "cache_ttl_seconds", 3600
                    );
                    
                    logger.debug("📊 Informações da chave geradas: kid={}", key.get("kid"));
                    
                    return ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                        .body(keyInfo);
                }
                
                return ResponseEntity.ok()
                    .body(Map.of("error", "no_key_available"));
            })
            .doOnError(error -> 
                logger.error("❌ Erro ao obter informações da chave: {}", error.getMessage())
            )
            .onErrorReturn(
                ResponseEntity.status(500)
                    .body(Map.of("error", "internal_server_error"))
            );
    }
    
    /**
     * ✨ Endpoint de health check específico para JWKS.
     */
    @GetMapping(value = "/oauth2/jwks/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Health check do serviço JWKS",
        description = "Verifica se o serviço JWKS está funcionando corretamente"
    )
    @ApiResponse(responseCode = "200", description = "Serviço JWKS está saudável")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
        logger.debug("✨ Health check JWKS solicitado");
        
        return jwtService.generateJwkSet()
            .map(jwkSet -> {
                @SuppressWarnings("unchecked")
                var keys = (java.util.List<Map<String, Object>>) jwkSet.get("keys");
                boolean hasKeys = keys != null && !keys.isEmpty();
                
                Map<String, Object> health = Map.of(
                    "status", hasKeys ? "UP" : "DOWN",
                    "service", "jwks",
                    "keys_available", hasKeys,
                    "key_count", hasKeys ? keys.size() : 0,
                    "timestamp", System.currentTimeMillis()
                );
                
                return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(health);
            })
            .doOnSuccess(response -> 
                logger.debug("✨ Health check JWKS concluído")
            )
            .doOnError(error -> 
                logger.warn("⚠️ Health check JWKS falhou: {}", error.getMessage())
            )
            .onErrorReturn(
                ResponseEntity.status(500)
                    .body(Map.of(
                        "status", "DOWN",
                        "service", "jwks",
                        "error", "service_unavailable"
                    ))
            );
    }
}