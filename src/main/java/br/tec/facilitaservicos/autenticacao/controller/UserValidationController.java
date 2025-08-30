package br.tec.facilitaservicos.autenticacao.controller;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.tec.facilitaservicos.autenticacao.dto.TokenValidationResponseDTO;
import br.tec.facilitaservicos.autenticacao.dto.UserStatusDTO;
import br.tec.facilitaservicos.autenticacao.dto.UsuarioDTO;
import br.tec.facilitaservicos.autenticacao.service.AuthService;
import br.tec.facilitaservicos.autenticacao.service.UserValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import reactor.core.publisher.Mono;

/**
 * ============================================================================
 * 🔐 CONTROLADOR DE VALIDAÇÃO DE USUÁRIOS - INTER-SERVICE COMMUNICATION
 * ============================================================================
 * 
 * Controlador específico para comunicação entre microserviços.
 * Fornece endpoints para validação de tokens e informações de usuários
 * que outros serviços podem consumir de forma reativa.
 * 
 * Endpoints disponíveis:
 * - POST /api/v1/auth/validate - Validação de tokens JWT
 * - GET /api/v1/users/{userId} - Informações do usuário
 * - GET /api/v1/users/{userId}/status - Status online do usuário
 * - GET /api/v1/users/{userId}/permissions - Permissões do usuário
 * 
 * Características:
 * - Otimizado para chamadas inter-service
 * - Cache inteligente para performance
 * - Circuit breakers integrados
 * - Observabilidade completa
 * ============================================================================
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "User Validation", description = "Endpoints para validação entre microserviços")
public class UserValidationController {

    private static final Logger logger = LoggerFactory.getLogger(UserValidationController.class);

    // ============================================================================
    // 🔧 CONSTANTES DE CONFIGURAÇÃO - AMBIENTE DE PRODUÇÃO
    // ============================================================================

    // Endpoints
    private static final String AUTH_VALIDATE_ENDPOINT = "/auth/validate";
    private static final String USERS_ENDPOINT = "/users/{userId}";
    private static final String USER_STATUS_ENDPOINT = "/users/{userId}/status";
    private static final String USER_PERMISSIONS_ENDPOINT = "/users/{userId}/permissions";
    private static final String HEALTH_ENDPOINT = "/users/health";

    // Headers
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_CACHE_CONTROL = "Cache-Control";
    private static final String BEARER_PREFIX = "Bearer ";

    // Cache TTL
    private static final String CACHE_PRIVATE_5MIN = "private, max-age=300";
    private static final String CACHE_PRIVATE_10MIN = "private, max-age=600";
    private static final String CACHE_PRIVATE_1MIN = "private, max-age=60";
    private static final String CACHE_PRIVATE_30MIN = "private, max-age=1800";

    // Chaves de resposta JSON
    private static final String JSON_KEY_STATUS = "status";
    private static final String JSON_KEY_COMPONENT = "component";
    private static final String JSON_KEY_TIMESTAMP = "timestamp";

    // Valores de status
    private static final String STATUS_DOWN = "DOWN";
    private static final String STATUS_UNKNOWN = "UNKNOWN";
    private static final String COMPONENT_USER_VALIDATION = "user-validation";

    // Mensagens de erro
    private static final String MSG_INVALID_AUTH_HEADER = "Invalid Authorization header format";
    private static final String MSG_EMPTY_TOKEN = "Empty token";
    private static final String MSG_FAILED_TO_VALIDATE = "Failed to validate token";
    private static final String MSG_UNKNOWN_ERROR = "Erro desconhecido";

    // Valores numéricos
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final long OFFLINE_FALLBACK_SECONDS = 3600L; // 1 hora

    private final AuthService authService;
    private final UserValidationService userValidationService;

    public UserValidationController(AuthService authService, UserValidationService userValidationService) {
        this.authService = authService;
        this.userValidationService = userValidationService;
    }

    /**
     * 🔍 Endpoint para validação de tokens por outros microserviços.
     * 
     * Usado pelos outros serviços para verificar se um token JWT é válido
     * e obter informações básicas do usuário sem precisar validar localmente.
     */
    @PostMapping(value = AUTH_VALIDATE_ENDPOINT,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Validação de token JWT para microserviços",
        description = "Valida token JWT e retorna informações do usuário para comunicação inter-service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token válido - retorna dados do usuário",
            content = @Content(schema = @Schema(implementation = TokenValidationResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token inválido ou expirado",
            content = @Content(schema = @Schema(implementation = TokenValidationResponseDTO.class))
        )
    })
    public Mono<ResponseEntity<TokenValidationResponseDTO>> validateToken(
            @RequestHeader(HEADER_AUTHORIZATION) String authorizationHeader) {

        logger.debug("🔍 Validação de token solicitada por microserviço");

        // Programação defensiva: validação do serviço
        if (userValidationService == null) {
            logger.error("❌ UserValidationService não está disponível");
            TokenValidationResponseDTO errorResponse = TokenValidationResponseDTO.builder()
                .valid(false)
                .errorMessage(MSG_FAILED_TO_VALIDATE)
                .build();
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        }

        return extractTokenFromHeader(authorizationHeader)
            .flatMap(token -> userValidationService.validateTokenForServices(token))
            .map(response -> {
                // Programação defensiva: validação da resposta
                if (response == null) {
                    logger.warn("⚠️ UserValidationService retornou resposta nula");
                    TokenValidationResponseDTO fallbackResponse = TokenValidationResponseDTO.builder()
                        .valid(false)
                        .errorMessage(MSG_FAILED_TO_VALIDATE)
                        .build();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(fallbackResponse);
                }

                HttpStatus status = response.isValid() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
                logger.debug("✅ Token validation result: valid={}, userId={}",
                           response.isValid(), response.getUserId());

                return ResponseEntity.status(status)
                    .header(HEADER_CACHE_CONTROL, CACHE_PRIVATE_5MIN) // Cache 5 min se válido
                    .body(response);
            })
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.warn("❌ Erro na validação de token: {}", errorMsg);

                TokenValidationResponseDTO invalidResponse = TokenValidationResponseDTO.builder()
                    .valid(false)
                    .errorMessage(MSG_FAILED_TO_VALIDATE)
                    .build();

                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(invalidResponse));
            });
    }

    /**
     * 👤 Endpoint para obter informações do usuário por ID.
     * 
     * Usado por outros microserviços para obter dados básicos de um usuário
     * com base no ID extraído do token JWT.
     */
    @GetMapping(value = USERS_ENDPOINT,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Obter informações do usuário",
        description = "Retorna informações básicas do usuário para comunicação inter-service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Usuário encontrado",
            content = @Content(schema = @Schema(implementation = UsuarioDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Usuário não encontrado"
        )
    })
    @PreAuthorize("hasAuthority('SCOPE_USER_READ') or authentication.name == #userId.toString()")
    public Mono<ResponseEntity<UsuarioDTO>> getUserInfo(@PathVariable @NotNull Long userId) {

        // Programação defensiva: validação de parâmetros
        if (userId == null || userId <= 0) {
            logger.warn("❌ UserId inválido recebido: {}", userId);
            return Mono.just(ResponseEntity.badRequest().<UsuarioDTO>build());
        }

        if (userValidationService == null) {
            logger.error("❌ UserValidationService não está disponível para getUserInfo");
            return Mono.just(ResponseEntity.internalServerError().<UsuarioDTO>build());
        }

        logger.debug("👤 Informações do usuário solicitadas: userId={}", userId);

        return userValidationService.getUserById(userId)
            .map(user -> {
                // Programação defensiva: validação do usuário retornado
                if (user == null) {
                    logger.warn("⚠️ UserValidationService retornou usuário nulo para userId={}", userId);
                    return ResponseEntity.notFound().<UsuarioDTO>build();
                }

                logger.debug("✅ Usuário encontrado: userId={}, username={}",
                           user.getId(), user.getUsername());

                return ResponseEntity.ok()
                    .header(HEADER_CACHE_CONTROL, CACHE_PRIVATE_10MIN) // Cache 10 min
                    .body(user);
            })
            .switchIfEmpty(
                Mono.fromCallable(() -> {
                    logger.debug("❌ Usuário não encontrado: userId={}", userId);
                    return ResponseEntity.notFound().<UsuarioDTO>build();
                })
            )
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.error("❌ Erro ao buscar usuário {}: {}", userId, errorMsg);
                return Mono.just(ResponseEntity.internalServerError().<UsuarioDTO>build());
            });
    }

    /**
     * 🟢 Endpoint para verificar status online do usuário.
     * 
     * Usado principalmente pelo microserviço de chat para verificar
     * se um usuário está online antes de enviar mensagens.
     */
    @GetMapping(value = USER_STATUS_ENDPOINT,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Status online do usuário",
        description = "Verifica se o usuário está online e retorna último acesso"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Status do usuário obtido com sucesso",
            content = @Content(schema = @Schema(implementation = UserStatusDTO.class))
        )
    })
    public Mono<ResponseEntity<UserStatusDTO>> getUserStatus(@PathVariable @NotNull Long userId) {

        // Programação defensiva: validação de parâmetros
        if (userId == null || userId <= 0) {
            logger.warn("❌ UserId inválido recebido para status: {}", userId);
            return Mono.just(ResponseEntity.badRequest().<UserStatusDTO>build());
        }

        if (userValidationService == null) {
            logger.error("❌ UserValidationService não está disponível para getUserStatus");
            UserStatusDTO errorStatus = UserStatusDTO.builder()
                .userId(userId)
                .online(false)
                .lastSeen(Instant.now().minusSeconds(OFFLINE_FALLBACK_SECONDS))
                .status(STATUS_UNKNOWN)
                .build();
            return Mono.just(ResponseEntity.ok(errorStatus));
        }

        logger.debug("🟢 Status do usuário solicitado: userId={}", userId);

        return userValidationService.getUserStatus(userId)
            .map(status -> {
                // Programação defensiva: validação do status retornado
                if (status == null) {
                    logger.warn("⚠️ UserValidationService retornou status nulo para userId={}", userId);
                    UserStatusDTO fallbackStatus = UserStatusDTO.builder()
                        .userId(userId)
                        .online(false)
                        .lastSeen(Instant.now().minusSeconds(OFFLINE_FALLBACK_SECONDS))
                        .status(STATUS_UNKNOWN)
                        .build();
                    return ResponseEntity.ok(fallbackStatus);
                }

                logger.debug("✅ Status obtido: userId={}, online={}, lastSeen={}",
                           userId, status.isOnline(), status.getLastSeen());

                return ResponseEntity.ok()
                    .header(HEADER_CACHE_CONTROL, CACHE_PRIVATE_1MIN) // Cache 1 min apenas
                    .body(status);
            })
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.warn("❌ Erro ao obter status do usuário {}: {}", userId, errorMsg);

                // Retorna status offline em caso de erro
                UserStatusDTO offlineStatus = UserStatusDTO.builder()
                    .userId(userId)
                    .online(false)
                    .lastSeen(Instant.now().minusSeconds(OFFLINE_FALLBACK_SECONDS)) // 1 hora atrás
                    .status(STATUS_UNKNOWN)
                    .build();

                return Mono.just(ResponseEntity.ok(offlineStatus));
            });
    }

    /**
     * 🔑 Endpoint para obter permissões do usuário.
     * 
     * Usado para verificação de autorização granular em outros microserviços.
     */
    @GetMapping(value = USER_PERMISSIONS_ENDPOINT,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Permissões do usuário",
        description = "Retorna lista de permissões/roles do usuário"
    )
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or authentication.name == #userId.toString()")
    public Mono<ResponseEntity<Object>> getUserPermissions(@PathVariable @NotNull Long userId) {

        // Programação defensiva: validação de parâmetros
        if (userId == null || userId <= 0) {
            logger.warn("❌ UserId inválido recebido para permissões: {}", userId);
            return Mono.just(ResponseEntity.badRequest().build());
        }

        if (userValidationService == null) {
            logger.error("❌ UserValidationService não está disponível para getUserPermissions");
            return Mono.just(ResponseEntity.internalServerError().build());
        }

        logger.debug("🔑 Permissões do usuário solicitadas: userId={}", userId);

        return userValidationService.getUserPermissions(userId)
            .map(permissions -> {
                // Programação defensiva: validação das permissões retornadas
                if (permissions == null) {
                    logger.warn("⚠️ UserValidationService retornou permissões nulas para userId={}", userId);
                    return ResponseEntity.ok()
                        .header(HEADER_CACHE_CONTROL, CACHE_PRIVATE_30MIN)
                        .body((Object) java.util.Collections.emptyList());
                }

                logger.debug("✅ Permissões obtidas: userId={}, count={}",
                           userId, permissions.size());

                return ResponseEntity.ok()
                    .header(HEADER_CACHE_CONTROL, CACHE_PRIVATE_30MIN) // Cache 30 min
                    .body((Object) permissions);
            })
            .onErrorResume(throwable -> {
                String errorMsg = throwable != null ? throwable.getMessage() : MSG_UNKNOWN_ERROR;
                logger.error("❌ Erro ao obter permissões do usuário {}: {}", userId, errorMsg);
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }

    /**
     * 📊 Health check específico para validação de usuários.
     */
    @GetMapping(value = HEALTH_ENDPOINT,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Health check do serviço de validação",
        description = "Verifica saúde dos componentes de validação de usuários"
    )
    public Mono<ResponseEntity<Object>> healthCheck() {

        // Programação defensiva: validação do serviço
        if (userValidationService == null) {
            logger.error("❌ UserValidationService não está disponível para health check");
            java.util.Map<String, Object> errorHealth = java.util.Map.of(
                JSON_KEY_STATUS, STATUS_DOWN,
                JSON_KEY_COMPONENT, COMPONENT_USER_VALIDATION,
                JSON_KEY_TIMESTAMP, Instant.now().toString()
            );
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body((Object) errorHealth));
        }

        return userValidationService.performHealthCheck()
            .map(health -> {
                // Programação defensiva: validação da resposta de health
                if (health == null) {
                    logger.warn("⚠️ UserValidationService retornou health nulo");
                    java.util.Map<String, Object> fallbackHealth = java.util.Map.of(
                        JSON_KEY_STATUS, STATUS_DOWN,
                        JSON_KEY_COMPONENT, COMPONENT_USER_VALIDATION,
                        JSON_KEY_TIMESTAMP, Instant.now().toString()
                    );
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body((Object) fallbackHealth);
                }

                return ResponseEntity.ok().body((Object) health);
            })
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body((Object) java.util.Map.of(
                    JSON_KEY_STATUS, STATUS_DOWN,
                    JSON_KEY_COMPONENT, COMPONENT_USER_VALIDATION,
                    JSON_KEY_TIMESTAMP, Instant.now().toString()
                )));
    }

    // Métodos auxiliares privados

    private Mono<String> extractTokenFromHeader(String authorizationHeader) {
        // Programação defensiva: validação do header de autorização
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            logger.warn("❌ Header de autorização nulo ou vazio");
            return Mono.error(new IllegalArgumentException(MSG_INVALID_AUTH_HEADER));
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            logger.warn("❌ Header de autorização não contém prefixo Bearer: {}",
                authorizationHeader.substring(0, Math.min(authorizationHeader.length(), 20)));
            return Mono.error(new IllegalArgumentException(MSG_INVALID_AUTH_HEADER));
        }

        try {
            String token = authorizationHeader.substring(BEARER_PREFIX_LENGTH); // Remove "Bearer "
            if (token.trim().isEmpty()) {
                logger.warn("❌ Token vazio após remoção do prefixo Bearer");
                return Mono.error(new IllegalArgumentException(MSG_EMPTY_TOKEN));
            }

            return Mono.just(token.trim());
        } catch (StringIndexOutOfBoundsException e) {
            logger.warn("❌ Erro ao extrair token do header: {}", e.getMessage());
            return Mono.error(new IllegalArgumentException(MSG_INVALID_AUTH_HEADER));
        }
    }
}