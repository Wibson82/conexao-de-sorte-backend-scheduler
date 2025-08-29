package br.tec.facilitaservicos.autenticacao.service;

import br.tec.facilitaservicos.autenticacao.dto.TokenValidationResponseDTO;
import br.tec.facilitaservicos.autenticacao.dto.UserStatusDTO;
import br.tec.facilitaservicos.autenticacao.dto.UsuarioDTO;
import br.tec.facilitaservicos.autenticacao.entity.Usuario;
import br.tec.facilitaservicos.autenticacao.repository.UsuarioRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * 🔐 SERVIÇO DE VALIDAÇÃO DE USUÁRIOS - INTER-SERVICE COMMUNICATION
 * ============================================================================
 * 
 * Serviço responsável por fornecer informações de usuários para outros
 * microserviços de forma otimizada, com cache e resilience patterns.
 * 
 * Características:
 * - Cache inteligente para reduzir latência
 * - Circuit breakers para resiliência
 * - Tracking de sessões/status online
 * - Otimizado para alta performance
 * ============================================================================
 */
@Service
public class UserValidationService {

    private static final Logger logger = LoggerFactory.getLogger(UserValidationService.class);

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    
    // Cache em memória para status online (complementar ao Redis)
    private final ConcurrentHashMap<Long, UserSessionInfo> userSessions = new ConcurrentHashMap<>();

    public UserValidationService(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * 🔍 Valida token JWT e retorna informações do usuário para outros microserviços.
     */
    @CircuitBreaker(name = "token-validation", fallbackMethod = "fallbackValidateToken")
    @Retry(name = "token-validation")
    @TimeLimiter(name = "token-validation")
    public Mono<TokenValidationResponseDTO> validateTokenForServices(String token) {
        logger.debug("🔍 Validando token para comunicação inter-service");
        
        return jwtService.validateAccessToken(token)
            .flatMap(claims -> {
                Long userId = Long.valueOf(claims.getSubject());
                String username = (String) claims.getClaim("preferred_username");
                
                return getUserById(userId)
                    .map(user -> TokenValidationResponseDTO.valid(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getRoles(),
                        user.getPermissions(),
                        claims.getExpirationTime().toInstant(),
                        claims.getSubject(),
                        claims.getIssuer()
                    ))
                    .doOnSuccess(response -> {
                        // Atualizar status online do usuário
                        updateUserOnlineStatus(userId, username);
                        logger.debug("✅ Token validado com sucesso: userId={}", userId);
                    });
            })
            .onErrorReturn(TokenValidationResponseDTO.invalid("Token validation failed"))
            .timeout(Duration.ofSeconds(3));
    }

    /**
     * 👤 Busca informações do usuário por ID com cache.
     */
    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    @CircuitBreaker(name = "user-lookup", fallbackMethod = "fallbackGetUser")
    @Retry(name = "user-lookup")
    public Mono<UsuarioDTO> getUserById(Long userId) {
        logger.debug("👤 Buscando usuário: userId={}", userId);
        
        return usuarioRepository.findById(userId)
            .map(UsuarioDTO::from)
            .doOnSuccess(user -> logger.debug("✅ Usuário encontrado: {}", user.toLogString()))
            .timeout(Duration.ofSeconds(2));
    }

    /**
     * 🟢 Obtém status online do usuário.
     */
    @CircuitBreaker(name = "user-status", fallbackMethod = "fallbackGetUserStatus")
    public Mono<UserStatusDTO> getUserStatus(Long userId) {
        logger.debug("🟢 Verificando status do usuário: userId={}", userId);
        
        UserSessionInfo sessionInfo = userSessions.get(userId);
        
        if (sessionInfo != null && sessionInfo.isActive()) {
            return Mono.just(UserStatusDTO.online(
                userId,
                sessionInfo.getStatusMessage(),
                sessionInfo.getDevice(),
                sessionInfo.getIpAddress()
            ));
        }
        
        // Buscar última atividade no banco se não estiver em sessão ativa
        return usuarioRepository.findById(userId)
            .map(usuario -> UserStatusDTO.offline(userId, 
                usuario.getUltimoLogin() != null ? 
                    usuario.getUltimoLogin().atZone(java.time.ZoneOffset.UTC).toInstant() : 
                    Instant.now().minusSeconds(3600)))
            .switchIfEmpty(Mono.just(UserStatusDTO.offline(userId, Instant.now().minusSeconds(3600))))
            .timeout(Duration.ofSeconds(1));
    }

    /**
     * 🔑 Obtém permissões do usuário.
     */
    @Cacheable(value = "user-permissions", key = "#userId", unless = "#result == null")
    @CircuitBreaker(name = "user-permissions", fallbackMethod = "fallbackGetUserPermissions")
    public Mono<Map<String, Object>> getUserPermissions(Long userId) {
        logger.debug("🔑 Obtendo permissões do usuário: userId={}", userId);
        
        return usuarioRepository.findById(userId)
            .map(usuario -> Map.<String, Object>of(
                "userId", usuario.getId(),
                "roles", usuario.getRoles(),
                "permissions", usuario.getPermissoes(),
                "isAdmin", usuario.getRoles().contains("ROLE_ADMIN"),
                "isPremium", usuario.getRoles().contains("ROLE_PREMIUM"),
                "canChat", usuario.getPermissoes().contains("chat:send"),
                "canModerate", usuario.getPermissoes().contains("chat:moderate")
            ))
            .timeout(Duration.ofSeconds(2));
    }

    /**
     * 📊 Realiza health check dos componentes de validação.
     */
    public Mono<Map<String, Object>> performHealthCheck() {
        Instant startTime = Instant.now();
        
        return usuarioRepository.count()
            .map(userCount -> {
                Duration responseTime = Duration.between(startTime, Instant.now());
                
                return Map.<String, Object>of(
                    "status", "UP",
                    "component", "user-validation-service",
                    "checks", Map.of(
                        "database", Map.of(
                            "status", "UP",
                            "responseTime", responseTime.toMillis() + "ms",
                            "userCount", userCount
                        ),
                        "cache", Map.of(
                            "status", "UP",
                            "activeSessions", userSessions.size()
                        ),
                        "jwt", Map.of(
                            "status", "UP",
                            "provider", "available"
                        )
                    ),
                    "timestamp", Instant.now().toString()
                );
            })
            .timeout(Duration.ofSeconds(5))
            .onErrorReturn(Map.of(
                "status", "DOWN",
                "component", "user-validation-service",
                "error", "Health check failed",
                "timestamp", Instant.now().toString()
            ));
    }

    /**
     * 🔄 Atualiza status online do usuário.
     */
    public void updateUserOnlineStatus(Long userId, String username) {
        updateUserOnlineStatus(userId, username, null, null, null);
    }

    /**
     * 🔄 Atualiza status online do usuário com informações detalhadas.
     */
    public void updateUserOnlineStatus(Long userId, String username, String statusMessage, 
                                     String device, String ipAddress) {
        UserSessionInfo sessionInfo = UserSessionInfo.builder()
            .userId(userId)
            .username(username)
            .statusMessage(statusMessage)
            .device(device)
            .ipAddress(ipAddress)
            .lastActivity(Instant.now())
            .build();
            
        userSessions.put(userId, sessionInfo);
        
        logger.debug("🔄 Status online atualizado: userId={}, device={}", userId, device);
    }

    /**
     * 🚪 Marca usuário como offline.
     */
    public void markUserOffline(Long userId) {
        userSessions.remove(userId);
        logger.debug("🚪 Usuário marcado como offline: userId={}", userId);
    }

    /**
     * 🧹 Limpa sessões expiradas (executado periodicamente).
     */
    public void cleanExpiredSessions() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(15)); // 15 minutos de inatividade
        
        int removed = 0;
        var iterator = userSessions.entrySet().iterator();
        
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().getLastActivity().isBefore(cutoff)) {
                iterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            logger.info("🧹 Limpeza de sessões: {} sessões expiradas removidas", removed);
        }
    }

    // Métodos de fallback para resilience

    public Mono<TokenValidationResponseDTO> fallbackValidateToken(String token, Exception ex) {
        logger.warn("🔴 Fallback ativado para validação de token: {}", ex.getMessage());
        return Mono.just(TokenValidationResponseDTO.invalid("Token validation service temporarily unavailable"));
    }

    public Mono<UsuarioDTO> fallbackGetUser(Long userId, Exception ex) {
        logger.warn("🔴 Fallback ativado para busca de usuário {}: {}", userId, ex.getMessage());
        return Mono.just(UsuarioDTO.anonimo(userId));
    }

    public Mono<UserStatusDTO> fallbackGetUserStatus(Long userId, Exception ex) {
        logger.warn("🔴 Fallback ativado para status do usuário {}: {}", userId, ex.getMessage());
        return Mono.just(UserStatusDTO.offline(userId, Instant.now().minusSeconds(3600)));
    }

    public Mono<Map<String, Object>> fallbackGetUserPermissions(Long userId, Exception ex) {
        logger.warn("🔴 Fallback ativado para permissões do usuário {}: {}", userId, ex.getMessage());
        return Mono.just(Map.of(
            "userId", userId,
            "roles", List.of("ROLE_USER"),
            "permissions", List.of("basic:access"),
            "isAdmin", false,
            "isPremium", false,
            "canChat", true,
            "canModerate", false
        ));
    }

    /**
     * Classe interna para informações de sessão do usuário.
     */
    private static class UserSessionInfo {
        private final Long userId;
        private final String username;
        private final String statusMessage;
        private final String device;
        private final String ipAddress;
        private final Instant lastActivity;

        private UserSessionInfo(Builder builder) {
            this.userId = builder.userId;
            this.username = builder.username;
            this.statusMessage = builder.statusMessage;
            this.device = builder.device;
            this.ipAddress = builder.ipAddress;
            this.lastActivity = builder.lastActivity;
        }

        public static Builder builder() {
            return new Builder();
        }

        public boolean isActive() {
            return lastActivity.isAfter(Instant.now().minus(Duration.ofMinutes(5)));
        }

        // Getters
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getStatusMessage() { return statusMessage; }
        public String getDevice() { return device; }
        public String getIpAddress() { return ipAddress; }
        public Instant getLastActivity() { return lastActivity; }

        public static class Builder {
            private Long userId;
            private String username;
            private String statusMessage;
            private String device;
            private String ipAddress;
            private Instant lastActivity;

            public Builder userId(Long userId) { this.userId = userId; return this; }
            public Builder username(String username) { this.username = username; return this; }
            public Builder statusMessage(String statusMessage) { this.statusMessage = statusMessage; return this; }
            public Builder device(String device) { this.device = device; return this; }
            public Builder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
            public Builder lastActivity(Instant lastActivity) { this.lastActivity = lastActivity; return this; }

            public UserSessionInfo build() { return new UserSessionInfo(this); }
        }
    }
}