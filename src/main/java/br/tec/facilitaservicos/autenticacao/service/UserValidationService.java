package br.tec.facilitaservicos.autenticacao.service;

import br.tec.facilitaservicos.autenticacao.dto.UsuarioDTO;
import br.tec.facilitaservicos.autenticacao.dto.TokenValidationResponse;
import br.tec.facilitaservicos.autenticacao.dto.UserStatusDTO;
import br.tec.facilitaservicos.autenticacao.exception.AuthenticationException;
import br.tec.facilitaservicos.autenticacao.client.UserServiceClient;
import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;

/**
 * Serviço para validação de usuários durante o processo de autenticação.
 */
@Service
public class UserValidationService {

    private final PasswordEncoder passwordEncoder;
    private final UserServiceClient userServiceClient;
    private final JwtService jwtService;

    public UserValidationService(PasswordEncoder passwordEncoder, 
                               UserServiceClient userServiceClient,
                               JwtService jwtService) {
        this.passwordEncoder = passwordEncoder;
        this.userServiceClient = userServiceClient;
        this.jwtService = jwtService;
    }

    /**
     * Valida a senha e o estado do usuário.
     *
     * @param usuario O DTO do usuário a ser validado.
     * @param senha A senha fornecida pelo usuário.
     * @return Um Mono contendo o UsuarioDTO se a validação for bem-sucedida, ou um Mono.error com AuthenticationException caso contrário.
     */
    public Mono<UsuarioDTO> validateUser(UsuarioDTO usuario, String senha) {
        return Mono.fromCallable(() -> {
            // Validar senha
            if (!passwordEncoder.matches(senha, usuario.getPassword())) {
                throw new AuthenticationException("Senha inválida");
            }

            // Validar estado do usuário
            if (!usuario.isAtivo() || usuario.isContaBloqueada() || !usuario.isEmailVerificado()) {
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
        });
    }

    /**
     * Valida token para serviços.
     */
    public Mono<TokenValidationResponse> validateTokenForServices(String token) {
        return jwtService.validateAccessToken(token)
            .flatMap(claims -> {
                Long userId = Long.parseLong(claims.getSubject());
                return userServiceClient.findById(userId)
                    .map(usuario -> createValidTokenResponse(usuario, claims))
                    .defaultIfEmpty(new TokenValidationResponse("User not found"));
            })
            .onErrorReturn(new TokenValidationResponse("Token validation failed"));
    }

    /**
     * Busca usuário por ID.
     */
    public Mono<UsuarioDTO> getUserById(Long userId) {
        return userServiceClient.findById(userId)
            .defaultIfEmpty(createAnonymousUser(userId));
    }

    /**
     * Obtém status do usuário.
     */
    public Mono<UserStatusDTO> getUserStatus(Long userId) {
        return userServiceClient.findById(userId)
            .map(usuario -> new UserStatusDTO(
                userId,
                determineUserStatus(usuario),
                usuario.isAtivo(),
                usuario.isContaBloqueada(),
                usuario.isEmailVerificado()
            ))
            .defaultIfEmpty(new UserStatusDTO(userId, "NOT_FOUND"));
    }

    /**
     * Fallback para validação de token.
     */
    public Mono<TokenValidationResponse> fallbackValidateToken(String token, RuntimeException exception) {
        return Mono.just(new TokenValidationResponse("Token validation service temporarily unavailable"));
    }

    /**
     * Fallback para busca de usuário.
     */
    public Mono<UsuarioDTO> fallbackGetUser(Long userId, RuntimeException exception) {
        return Mono.just(createAnonymousUser(userId));
    }

    // Métodos auxiliares privados
    
    private TokenValidationResponse createValidTokenResponse(UsuarioDTO usuario, JWTClaimsSet claims) {
        LocalDateTime expiresAt = null;
        if (claims.getExpirationTime() != null) {
            expiresAt = LocalDateTime.ofInstant(
                claims.getExpirationTime().toInstant(), 
                ZoneId.systemDefault()
            );
        }
        
        return new TokenValidationResponse(
            usuario.getId(),
            usuario.getEmail(),
            usuario.getEmail(),
            usuario.getRoles(),
            usuario.getPermissoes(),
            expiresAt,
            claims.getSubject(),
            claims.getIssuer()
        );
    }
    
    private UsuarioDTO createAnonymousUser(Long userId) {
        return new UsuarioDTO(
            userId,
            "anonimo@exemplo.com",
            "Usuário Anônimo",
            null,
            "Usuário",
            "Anônimo",
            Set.of("ANONYMOUS"),
            Set.of("read"),
            false,
            false,
            null,
            0,
            false,
            null,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    
    private String determineUserStatus(UsuarioDTO usuario) {
        if (usuario.isContaBloqueada()) {
            return "BLOCKED";
        }
        if (!usuario.isAtivo()) {
            return "INACTIVE";
        }
        if (!usuario.isEmailVerificado()) {
            return "EMAIL_NOT_VERIFIED";
        }
        return "ACTIVE";
    }
}
