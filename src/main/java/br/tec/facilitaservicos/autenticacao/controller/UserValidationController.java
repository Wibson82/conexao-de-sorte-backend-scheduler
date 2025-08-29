package br.tec.facilitaservicos.autenticacao.controller;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;

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
    @PostMapping(value = "/auth/validate",
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
            @RequestHeader("Authorization") String authorizationHeader) {

        logger.debug("🔍 Validação de token solicitada por microserviço");

        return extractTokenFromHeader(authorizationHeader)
            .flatMap(token -> userValidationService.validateTokenForServices(token))
            .map(response -> {
                HttpStatus status = response.isValid() ? HttpStatus.OK : HttpStatus.UNAUTHORIZED;
                logger.debug("✅ Token validation result: valid={}, userId={}", 
                           response.isValid(), response.getUserId());
                
                return ResponseEntity.status(status)
                    .header("Cache-Control", "private, max-age=300") // Cache 5 min se válido
                    .body(response);
            })
            .onErrorResume(throwable -> {
                logger.warn("❌ Erro na validação de token: {}", throwable.getMessage());
                
                TokenValidationResponseDTO invalidResponse = TokenValidationResponseDTO.builder()
                    .valid(false)
                    .errorMessage("Failed to validate token")
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
    @GetMapping(value = "/users/{userId}",
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
        
        logger.debug("👤 Informações do usuário solicitadas: userId={}", userId);
        
        return userValidationService.getUserById(userId)
            .map(user -> {
                logger.debug("✅ Usuário encontrado: userId={}, username={}", 
                           user.getId(), user.getUsername());
                
                return ResponseEntity.ok()
                    .header("Cache-Control", "private, max-age=600") // Cache 10 min
                    .body(user);
            })
            .switchIfEmpty(
                Mono.fromCallable(() -> {
                    logger.debug("❌ Usuário não encontrado: userId={}", userId);
                    return ResponseEntity.notFound().<UsuarioDTO>build();
                })
            )
            .onErrorResume(throwable -> {
                logger.error("❌ Erro ao buscar usuário {}: {}", userId, throwable.getMessage());
                return Mono.just(ResponseEntity.internalServerError().<UsuarioDTO>build());
            });
    }

    /**
     * 🟢 Endpoint para verificar status online do usuário.
     * 
     * Usado principalmente pelo microserviço de chat para verificar
     * se um usuário está online antes de enviar mensagens.
     */
    @GetMapping(value = "/users/{userId}/status",
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
        
        logger.debug("🟢 Status do usuário solicitado: userId={}", userId);
        
        return userValidationService.getUserStatus(userId)
            .map(status -> {
                logger.debug("✅ Status obtido: userId={}, online={}, lastSeen={}", 
                           userId, status.isOnline(), status.getLastSeen());
                
                return ResponseEntity.ok()
                    .header("Cache-Control", "private, max-age=60") // Cache 1 min apenas
                    .body(status);
            })
            .onErrorResume(throwable -> {
                logger.warn("❌ Erro ao obter status do usuário {}: {}", userId, throwable.getMessage());
                
                // Retorna status offline em caso de erro
                UserStatusDTO offlineStatus = UserStatusDTO.builder()
                    .userId(userId)
                    .online(false)
                    .lastSeen(Instant.now().minusSeconds(3600)) // 1 hora atrás
                    .status("UNKNOWN")
                    .build();
                    
                return Mono.just(ResponseEntity.ok(offlineStatus));
            });
    }

    /**
     * 🔑 Endpoint para obter permissões do usuário.
     * 
     * Usado para verificação de autorização granular em outros microserviços.
     */
    @GetMapping(value = "/users/{userId}/permissions",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Permissões do usuário",
        description = "Retorna lista de permissões/roles do usuário"
    )
    @PreAuthorize("hasAuthority('SCOPE_ADMIN') or authentication.name == #userId.toString()")
    public Mono<ResponseEntity<Object>> getUserPermissions(@PathVariable @NotNull Long userId) {
        
        logger.debug("🔑 Permissões do usuário solicitadas: userId={}", userId);
        
        return userValidationService.getUserPermissions(userId)
            .map(permissions -> {
                logger.debug("✅ Permissões obtidas: userId={}, count={}", 
                           userId, permissions.size());
                
                return ResponseEntity.ok()
                    .header("Cache-Control", "private, max-age=1800") // Cache 30 min
                    .body((Object) permissions);
            })
            .onErrorResume(throwable -> {
                logger.error("❌ Erro ao obter permissões do usuário {}: {}", userId, throwable.getMessage());
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }

    /**
     * 📊 Health check específico para validação de usuários.
     */
    @GetMapping(value = "/users/health",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Health check do serviço de validação",
        description = "Verifica saúde dos componentes de validação de usuários"
    )
    public Mono<ResponseEntity<Object>> healthCheck() {
        
        return userValidationService.performHealthCheck()
            .map(health -> ResponseEntity.ok().body((Object) health))
            .onErrorReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body((Object) java.util.Map.of(
                    "status", "DOWN",
                    "component", "user-validation",
                    "timestamp", Instant.now().toString()
                )));
    }

    // Métodos auxiliares privados

    private Mono<String> extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Mono.error(new IllegalArgumentException("Invalid Authorization header format"));
        }
        
        String token = authorizationHeader.substring(7); // Remove "Bearer "
        if (token.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Empty token"));
        }
        
        return Mono.just(token);
    }
}