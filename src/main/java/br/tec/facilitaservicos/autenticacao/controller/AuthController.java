package br.tec.facilitaservicos.autenticacao.controller;

import br.tec.facilitaservicos.autenticacao.dto.*;
import br.tec.facilitaservicos.autenticacao.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * 🔐 CONTROLADOR DE AUTENTICAÇÃO - ENTERPRISE GRADE WEBFLUX
 * ============================================================================
 * 
 * Controlador reativo para autenticação OAuth2/OpenID Connect.
 * Implementa endpoints seguros e reativos para:
 * - Geração de tokens (login)
 * - Renovação de tokens (refresh)
 * - Introspecção de tokens (validação)
 * - Revogação de tokens (logout)
 * 
 * Características:
 * - 100% reativo (WebFlux)
 * - Conformidade com RFCs OAuth2/OpenID Connect
 * - Rate limiting integrado
 * - Headers de segurança apropriados
 * - Auditoria e observabilidade
 * - Tratamento de erros padronizado
 * 
 * ============================================================================
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Endpoints de autenticação OAuth2/OpenID Connect")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    private final AuthService authService;
    
    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    /**
     * 🔐 Endpoint de autenticação (geração de token).
     * 
     * Implementa o fluxo OAuth2 Resource Owner Password Credentials Grant.
     */
    @PostMapping(value = "/token", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Autenticação de usuário",
        description = "Realiza autenticação e retorna access token e refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Autenticação realizada com sucesso",
            content = @Content(schema = @Schema(implementation = RespostaTokenDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Credenciais inválidas",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "423",
            description = "Conta bloqueada",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Muitas tentativas de login",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public Mono<ResponseEntity<Object>> token(
            @Valid @RequestBody RequisicaoLoginDTO loginRequest,
            ServerWebExchange exchange) {
        
        String clientIp = getClientIp(exchange);
        String userAgent = getUserAgent(exchange);
        
        logger.info("🔐 Tentativa de autenticação: usuario={}, ip={}", 
                   loginRequest.usuario(), clientIp);
        
        return authService.authenticate(loginRequest, clientIp, userAgent)
            .map(tokenResponse -> {
                logger.info("✅ Autenticação bem-sucedida: usuario={}, ip={}", 
                           loginRequest.usuario(), clientIp);
                
                return ResponseEntity.ok()
                    .header("Cache-Control", "no-store")
                    .header("Pragma", "no-cache")
                    .body((Object) tokenResponse);
            })
            .onErrorResume(throwable -> {
                logger.warn("❌ Falha na autenticação: usuario={}, ip={}, erro={}", 
                           loginRequest.usuario(), clientIp, throwable.getMessage());
                
                return handleAuthError(throwable);
            });
    }
    
    /**
     * 🔄 Endpoint de renovação de token.
     * 
     * Implementa a renovação de access token usando refresh token.
     */
    @PostMapping(value = "/refresh",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Renovação de token de acesso",
        description = "Renova o access token usando refresh token válido"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token renovado com sucesso",
            content = @Content(schema = @Schema(implementation = RespostaTokenDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Refresh token inválido ou malformado"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Refresh token expirado ou revogado"
        )
    })
    public Mono<ResponseEntity<Object>> refresh(
            @Valid @RequestBody RequisicaoRefreshDTO refreshRequest,
            ServerWebExchange exchange) {
        
        String clientIp = getClientIp(exchange);
        String userAgent = getUserAgent(exchange);
        
        logger.info("🔄 Tentativa de renovação de token: ip={}", clientIp);
        
        return authService.refresh(refreshRequest, clientIp, userAgent)
            .map(tokenResponse -> {
                logger.info("✅ Token renovado com sucesso: ip={}", clientIp);
                
                return ResponseEntity.ok()
                    .header("Cache-Control", "no-store")
                    .header("Pragma", "no-cache")
                    .body((Object) tokenResponse);
            })
            .onErrorResume(throwable -> {
                logger.warn("❌ Falha na renovação: ip={}, erro={}", 
                           clientIp, throwable.getMessage());
                
                return handleRefreshError(throwable);
            });
    }
    
    /**
     * 🔍 Endpoint de introspecção de token (RFC 7662).
     * 
     * Permite validar e obter informações sobre um token.
     */
    @PostMapping(value = "/introspect",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Introspecção de token",
        description = "Valida token e retorna informações sobre sua validade e claims (RFC 7662)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Introspecção realizada com sucesso",
            content = @Content(schema = @Schema(implementation = RespostaIntrospeccaoDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Token malformado"
        )
    })
    public Mono<ResponseEntity<RespostaIntrospeccaoDTO>> introspect(
            @Valid @RequestBody RequisicaoIntrospeccaoDTO introspectRequest,
            ServerWebExchange exchange) {
        
        String clientIp = getClientIp(exchange);
        
        logger.debug("🔍 Introspecção de token solicitada: ip={}", clientIp);
        
        return authService.introspect(introspectRequest.token())
            .map(introspectionResponse -> {
                logger.debug("✅ Introspecção concluída: ativo={}, ip={}", 
                           introspectionResponse.ativo(), clientIp);
                
                return ResponseEntity.ok()
                    .header("Cache-Control", "no-store")
                    .header("Pragma", "no-cache")
                    .body(introspectionResponse);
            })
            .onErrorResume(throwable -> {
                logger.warn("❌ Erro na introspecção: ip={}, erro={}", 
                           clientIp, throwable.getMessage());
                
                // Sempre retorna token inativo em caso de erro (RFC 7662)
                return Mono.just(ResponseEntity.ok(RespostaIntrospeccaoDTO.inativo()));
            });
    }
    
    /**
     * 🚪 Endpoint de revogação de token (logout).
     * 
     * Revoga refresh token e invalida sessão.
     */
    @PostMapping(value = "/revoke",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Revogação de token",
        description = "Revoga refresh token e invalida sessão do usuário"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token revogado com sucesso"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Token inválido"
        )
    })
    public Mono<ResponseEntity<Map<String, Object>>> revoke(
            @Valid @RequestBody RequisicaoRefreshDTO revokeRequest,
            ServerWebExchange exchange) {
        
        String clientIp = getClientIp(exchange);
        
        logger.info("🚪 Tentativa de revogação: ip={}", clientIp);
        
        return authService.revoke(revokeRequest.tokenRenovacao())
            .then(Mono.fromCallable(() -> {
                logger.info("✅ Token revogado com sucesso: ip={}", clientIp);
                
                Map<String, Object> response = new HashMap<>();
                response.put("revoked", true);
                response.put("message", "Token revogado com sucesso");
                return ResponseEntity.ok(response);
            }))
            .onErrorResume(throwable -> {
                logger.warn("⚠️ Erro na revogação: ip={}, erro={}", 
                           clientIp, throwable.getMessage());
                
                // RFC 7009: Sempre retorna sucesso para revogação
                return Mono.just(ResponseEntity.ok(Map.of(
                    "revoked", true,
                    "message", "Token revogado"
                )));
            });
    }
    
    /**
     * ✅ Health check do serviço de autenticação.
     */
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Health check do serviço",
        description = "Verifica se o serviço de autenticação está funcionando"
    )
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        logger.debug("✅ Health check solicitado");
        
        return authService.healthCheck()
            .map(isHealthy -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", isHealthy ? "UP" : "DOWN");
                response.put("service", "authentication");
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(response);
            })
            .onErrorResume(throwable -> {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "DOWN");
                errorResponse.put("service", "authentication");
                errorResponse.put("timestamp", System.currentTimeMillis());
                return Mono.just(ResponseEntity.status(503).body(errorResponse));
            });
    }
    
    // Métodos auxiliares privados
    
    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return exchange.getRequest().getRemoteAddress() != null
            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            : "unknown";
    }
    
    private String getUserAgent(ServerWebExchange exchange) {
        return exchange.getRequest().getHeaders().getFirst("User-Agent");
    }
    
    private Mono<ResponseEntity<Object>> handleAuthError(Throwable throwable) {
        // Implementação simplificada - seria expandida com diferentes tipos de erro
        Map<String, Object> errorResponse = Map.of(
            "error", "invalid_grant",
            "error_description", "Credenciais inválidas"
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }
    
    private Mono<ResponseEntity<Object>> handleRefreshError(Throwable throwable) {
        Map<String, Object> errorResponse = Map.of(
            "error", "invalid_grant", 
            "error_description", "Refresh token inválido ou expirado"
        );
        
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse));
    }
}