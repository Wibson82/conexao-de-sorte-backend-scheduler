package br.tec.facilitaservicos.autenticacao.service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import br.tec.facilitaservicos.autenticacao.dto.UsuarioDTO;
import br.tec.facilitaservicos.autenticacao.exception.TokenException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Serviço para geração e validação de tokens JWT.
 * Implementação reativa usando RSA256 com chaves do Azure Key Vault.
 */
@Service
public class JwtService {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
    
    private final KeyVaultService keyVaultService;
    
    @Value("${jwt.access-token-validity:3600}")
    private long accessTokenValiditySeconds;
    
    @Value("${jwt.issuer:https://auth.conexaodesorte.com}")
    private String issuer;
    
    @Value("${jwt.audience:conexao-de-sorte}")
    private String audience;
    
    public JwtService(KeyVaultService keyVaultService) {
        this.keyVaultService = keyVaultService;
    }
    
    /**
     * Gera um access token JWT para o usuário.
     */
    public Mono<String> generateAccessToken(UsuarioDTO usuario) {
        logger.debug("Gerando access token para usuário: {}", usuario.getId());
        
        return keyVaultService.getPrivateKey()
            .zipWith(keyVaultService.getKeyId())
            .flatMap(tuple -> {
                RSAPrivateKey privateKey = tuple.getT1();
                String keyId = tuple.getT2();
                
                return Mono.fromCallable(() -> createAccessToken(usuario, privateKey, keyId))
                    .subscribeOn(Schedulers.boundedElastic());
            })
            .doOnSuccess(token -> logger.debug("Access token gerado com sucesso para usuário: {}", usuario.getId()))
            .doOnError(error -> logger.error("Erro ao gerar access token para usuário {}: {}", 
                                           usuario.getId(), error.getMessage()));
    }
    
    /**
     * Valida um access token e retorna as claims.
     */
    public Mono<JWTClaimsSet> validateAccessToken(String token) {
        logger.debug("Validando access token");
        
        return keyVaultService.getPublicKey()
            .flatMap(publicKey -> 
                Mono.fromCallable(() -> validateToken(token, publicKey))
                    .subscribeOn(Schedulers.boundedElastic())
            )
            .doOnSuccess(claims -> logger.debug("Access token validado com sucesso para subject: {}", 
                                             claims.getSubject()))
            .doOnError(error -> logger.warn("Erro ao validar access token: {}", error.getMessage()));
    }
    
    /**
     * Extrai o subject (usuário) do token sem validar assinatura.
     * Usado apenas para fins de logging/debug.
     */
    public Mono<String> extractSubjectUnsafe(String token) {
        return Mono.fromCallable(() -> {
            try {
                SignedJWT signedJWT = SignedJWT.parse(token);
                return signedJWT.getJWTClaimsSet().getSubject();
            } catch (Exception e) {
                logger.warn("Erro ao extrair subject do token: {}", e.getMessage());
                return null;
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
        /**
     * Gera JWK Set para o endpoint público.
     */
    public Mono<Map<String, Object>> generateJwkSet() {
        logger.debug("Gerando JWK Set");
        
        return keyVaultService.getPublicKey()
            .zipWith(keyVaultService.getKeyId())
            .flatMap(tuple -> {
                RSAPublicKey publicKey = tuple.getT1();
                String keyId = tuple.getT2();
                
                return Mono.fromCallable(() -> createJwkSet(publicKey, keyId))
                    .subscribeOn(Schedulers.boundedElastic());
            })
            .doOnSuccess(jwkSet -> logger.debug("JWK Set gerado com sucesso"))
            .doOnError(error -> logger.error("Erro ao gerar JWK Set: {}", error.getMessage()));
    }
    
    /**
     * Verifica se o token está expirado (sem validar assinatura).
     */
    public Mono<Boolean> isTokenExpired(String token) {
        return Mono.fromCallable(() -> {
            try {
                SignedJWT signedJWT = SignedJWT.parse(token);
                Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
                return expiration != null && expiration.before(new Date());
            } catch (ParseException e) {
                logger.warn("Erro ao verificar expiração do token: {}", e.getMessage());
                return true; // Considera expirado em caso de erro
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
    
    // Métodos privados auxiliares
    
    private String createAccessToken(UsuarioDTO usuario, RSAPrivateKey privateKey, String keyId) 
            throws JOSEException {
        
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessTokenValiditySeconds);
        
        // Claims do JWT
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
            .subject(String.valueOf(usuario.getId()))
            .issuer(issuer)
            .audience(audience)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(expiration))
            .notBeforeTime(Date.from(now))
            .claim("email", usuario.getEmail())
            .claim("username", usuario.getUsername())
            .claim("active", usuario.isAtivo())
            .claim("email_verified", usuario.isEmailVerificado())
            .claim("authorities", usuario.getRoles())
            .claim("scope", String.join(" ", usuario.getPermissoes()))
            .build();
        
        // Header do JWT
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
            .keyID(keyId)
            .type(JOSEObjectType.JWT)
            .build();
        
        // Criação e assinatura do JWT
        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        RSASSASigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);
        
        return signedJWT.serialize();
    }
    
    private JWTClaimsSet validateToken(String token, RSAPublicKey publicKey) 
            throws Exception {
        
        SignedJWT signedJWT = SignedJWT.parse(token);
        
        // Verificar assinatura
        RSASSAVerifier verifier = new RSASSAVerifier(publicKey);
        if (!signedJWT.verify(verifier)) {
            throw new TokenException("Assinatura do token inválida");
        }
        
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        
        // Verificar expiração
        Date expiration = claims.getExpirationTime();
        if (expiration != null && expiration.before(new Date())) {
            throw new TokenException("Token expirado");
        }
        
        // Verificar not before
        Date notBefore = claims.getNotBeforeTime();
        if (notBefore != null && notBefore.after(new Date())) {
            throw new TokenException("Token ainda não é válido");
        }
        
        // Verificar issuer
        if (!issuer.equals(claims.getIssuer())) {
            throw new TokenException("Issuer do token inválido");
        }
        
        // Verificar audience
        List<String> audiences = claims.getAudience();
        if (audiences == null || !audiences.contains(audience)) {
            throw new TokenException("Audience do token inválido");
        }
        
        return claims;
    }
    
    private Map<String, Object> createJwkSet(RSAPublicKey publicKey, String keyId) {
        
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .keyID(keyId)
            .algorithm(JWSAlgorithm.RS256)
            .keyUse(KeyUse.SIGNATURE)
            .build();
        
        return Map.of(
            "keys", List.of(rsaKey.toPublicJWK().toJSONObject())
        );
    }
}