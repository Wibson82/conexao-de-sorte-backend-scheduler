package br.tec.facilitaservicos.autenticacao.service;

import br.tec.facilitaservicos.autenticacao.dto.*;
import br.tec.facilitaservicos.autenticacao.entity.RefreshToken;
import br.tec.facilitaservicos.autenticacao.entity.Usuario;
import br.tec.facilitaservicos.autenticacao.exception.AuthenticationException;
import br.tec.facilitaservicos.autenticacao.repository.RefreshTokenRepository;
import br.tec.facilitaservicos.autenticacao.repository.UsuarioRepository;
import com.nimbusds.jwt.JWTClaimsSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Set;

/**
 * Serviço principal de autenticação.
 * Implementa lógica de negócio para autenticação reativa.
 */
@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    
    public AuthService(UsuarioRepository usuarioRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      JwtService jwtService,
                      PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Realiza autenticação de usuário.
     */
    public Mono<RespostaTokenDTO> authenticate(RequisicaoLoginDTO loginRequest, String clientIp, String userAgent) {
        logger.debug("Iniciando autenticação para usuário: {}", loginRequest.usuario());
        
        return usuarioRepository.findByEmailOrNomeUsuario(loginRequest.usuario())
            .switchIfEmpty(Mono.error(new AuthenticationException("Usuário não encontrado")))
            .flatMap(usuario -> validateUserAndGenerateTokens(usuario, loginRequest.senha(), clientIp, userAgent))
            .doOnSuccess(response -> logger.info("Autenticação concluída com sucesso"))
            .doOnError(error -> logger.error("Erro na autenticação: {}", error.getMessage()));
    }
    
    /**
     * Renova access token usando refresh token.
     */
    public Mono<RespostaTokenDTO> refresh(RequisicaoRefreshDTO refreshRequest, String clientIp, String userAgent) {
        logger.debug("Iniciando renovação de token");
        
        String refreshTokenHash = hashToken(refreshRequest.tokenRenovacao());
        
        return refreshTokenRepository.findByTokenHashAndAtivoTrueAndRevogadoFalse(refreshTokenHash)
            .switchIfEmpty(Mono.error(new AuthenticationException("Refresh token inválido")))
            .filter(refreshToken -> refreshToken.isValido())
            .switchIfEmpty(Mono.error(new AuthenticationException("Refresh token expirado")))
            .flatMap(refreshToken -> 
                usuarioRepository.findById(refreshToken.getUsuarioId())
                    .flatMap(usuario -> renewTokens(usuario, refreshToken, clientIp, userAgent))
            )
            .doOnSuccess(response -> logger.info("Token renovado com sucesso"))
            .doOnError(error -> logger.error("Erro na renovação: {}", error.getMessage()));
    }
    
    /**
     * Realiza introspecção de token.
     */
    public Mono<RespostaIntrospeccaoDTO> introspect(String token) {
        logger.debug("Iniciando introspecção de token");
        
        return jwtService.validateAccessToken(token)
            .map(claims -> createActiveIntrospectionResponse(claims))
            .onErrorReturn(RespostaIntrospeccaoDTO.inativo())
            .doOnNext(response -> logger.debug("Introspecção concluída: ativo={}", response.ativo()));
    }
    
    /**
     * Revoga refresh token.
     */
    public Mono<Void> revoke(String refreshToken) {
        logger.debug("Iniciando revogação de token");
        
        String refreshTokenHash = hashToken(refreshToken);
        
        return refreshTokenRepository.revokeToken(refreshTokenHash)
            .doOnSuccess(count -> logger.info("Token revogado: affected_rows={}", count))
            .then();
    }
    
    /**
     * Health check do serviço.
     */
    public Mono<Boolean> healthCheck() {
        return usuarioRepository.countUsuariosAtivos()
            .map(count -> count >= 0)
            .onErrorReturn(false);
    }
    
    // Métodos auxiliares privados
    
    private Mono<RespostaTokenDTO> validateUserAndGenerateTokens(Usuario usuario, String senha, 
                                                                 String clientIp, String userAgent) {
        return Mono.fromCallable(() -> {
            // Validar senha
            if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
                throw new AuthenticationException("Senha inválida");
            }
            
            // Validar estado do usuário
            if (!usuario.podeLogar()) {
                if (usuario.isContaBloqueada()) {
                    throw new AuthenticationException("Conta bloqueada");
                }
                if (!usuario.isAtivo()) {
                    throw new AuthenticationException("Conta inativa");
                }
                if (!usuario.isEmailVerificado()) {
                    throw new AuthenticationException("Email não verificado");
                }
            }
            
            return usuario;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(validatedUser -> generateTokenPair(validatedUser, clientIp, userAgent))
        .flatMap(tokenResponse -> 
            // Resetar tentativas falidas após sucesso
            usuarioRepository.updateTentativasLoginFalidas(usuario.getId(), 0)
                .thenReturn(tokenResponse)
        );
    }
    
    private Mono<RespostaTokenDTO> generateTokenPair(Usuario usuario, String clientIp, String userAgent) {
        return jwtService.generateAccessToken(usuario)
            .flatMap(accessToken -> {
                String refreshToken = generateSecureToken();
                String refreshTokenHash = hashToken(refreshToken);
                
                RefreshToken refreshTokenEntity = new RefreshToken(
                    refreshTokenHash,
                    usuario.getId(),
                    LocalDateTime.now().plusDays(7), // 7 dias
                    clientIp,
                    userAgent
                );
                
                return refreshTokenRepository.save(refreshTokenEntity)
                    .map(savedToken -> RespostaTokenDTO.of(
                        accessToken,
                        refreshToken,
                        3600L, // 1 hora
                        Set.of("ROLE_USER")
                    ));
            });
    }
    
    private Mono<RespostaTokenDTO> renewTokens(Usuario usuario, RefreshToken oldToken, 
                                              String clientIp, String userAgent) {
        return jwtService.generateAccessToken(usuario)
            .flatMap(accessToken -> {
                String newRefreshToken = generateSecureToken();
                String newRefreshTokenHash = hashToken(newRefreshToken);
                
                RefreshToken newTokenEntity = new RefreshToken(
                    newRefreshTokenHash,
                    usuario.getId(),
                    LocalDateTime.now().plusDays(7),
                    clientIp,
                    userAgent
                );
                newTokenEntity.setFamiliaToken(oldToken.getFamiliaToken());
                
                return refreshTokenRepository.save(newTokenEntity)
                    .flatMap(savedToken -> 
                        // Desativar o token antigo
                        refreshTokenRepository.deactivateToken(oldToken.getTokenHash())
                            .thenReturn(RespostaTokenDTO.of(
                                accessToken,
                                newRefreshToken,
                                3600L,
                                Set.of("ROLE_USER")
                            ))
                    );
            });
    }
    
    private RespostaIntrospeccaoDTO createActiveIntrospectionResponse(JWTClaimsSet claims) {
        try {
            return RespostaIntrospeccaoDTO.ativo(
                claims.getSubject(),
                claims.getIssuer(),
                claims.getAudience().get(0),
                claims.getExpirationTime().getTime() / 1000,
                claims.getIssueTime().getTime() / 1000,
                Set.of("ROLE_USER")
            );
        } catch (Exception e) {
            logger.warn("Erro ao criar resposta de introspecção: {}", e.getMessage());
            return RespostaIntrospeccaoDTO.inativo();
        }
    }
    
    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao fazer hash do token", e);
        }
    }
}