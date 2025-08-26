package br.tec.facilitaservicos.autenticacao.repository;

import br.tec.facilitaservicos.autenticacao.entity.RefreshToken;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Repositório R2DBC para entidade RefreshToken.
 * Gerencia tokens de renovação de forma reativa.
 */
@Repository
public interface RefreshTokenRepository extends R2dbcRepository<RefreshToken, Long> {
    
    /**
     * Busca refresh token ativo por hash
     */
    @Query("SELECT * FROM refresh_tokens WHERE token_hash = :tokenHash AND ativo = true AND revogado = false")
    Mono<RefreshToken> findByTokenHashAndAtivoTrueAndRevogadoFalse(@Param("tokenHash") String tokenHash);
    
    /**
     * Busca todos os refresh tokens de um usuário
     */
    Flux<RefreshToken> findByUsuarioId(Long usuarioId);
    
    /**
     * Busca refresh tokens ativos de um usuário
     */
    @Query("SELECT * FROM refresh_tokens WHERE usuario_id = :usuarioId AND ativo = true AND revogado = false")
    Flux<RefreshToken> findActiveTokensByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    /**
     * Busca refresh tokens válidos de um usuário (não expirados)
     */
    @Query("SELECT * FROM refresh_tokens WHERE usuario_id = :usuarioId AND ativo = true AND revogado = false AND data_expiracao > CURRENT_TIMESTAMP")
    Flux<RefreshToken> findValidTokensByUsuarioId(@Param("usuarioId") Long usuarioId);
    
    /**
     * Busca tokens da mesma família
     */
    Flux<RefreshToken> findByFamiliaToken(String familiaToken);
    
    /**
     * Busca tokens expirados
     */
    @Query("SELECT * FROM refresh_tokens WHERE data_expiracao < CURRENT_TIMESTAMP")
    Flux<RefreshToken> findExpiredTokens();
    
    /**
     * Busca tokens criados a partir de uma data
     */
    Flux<RefreshToken> findByDataCriacaoAfter(LocalDateTime data);
    
    /**
     * Busca tokens por IP de origem
     */
    Flux<RefreshToken> findByIpOrigem(String ipOrigem);
    
    /**
     * Revoga todos os tokens de um usuário
     */
    @Query("UPDATE refresh_tokens SET revogado = true, ativo = false, data_atualizacao = CURRENT_TIMESTAMP WHERE usuario_id = :usuarioId")
    Mono<Integer> revokeAllUserTokens(@Param("usuarioId") Long usuarioId);
    
    /**
     * Revoga todos os tokens de uma família
     */
    @Query("UPDATE refresh_tokens SET revogado = true, ativo = false, data_atualizacao = CURRENT_TIMESTAMP WHERE familia_token = :familiaToken")
    Mono<Integer> revokeTokenFamily(@Param("familiaToken") String familiaToken);
    
    /**
     * Revoga token específico por hash
     */
    @Query("UPDATE refresh_tokens SET revogado = true, ativo = false, data_atualizacao = CURRENT_TIMESTAMP WHERE token_hash = :tokenHash")
    Mono<Integer> revokeToken(@Param("tokenHash") String tokenHash);
    
    /**
     * Desativa token (para rotação)
     */
    @Query("UPDATE refresh_tokens SET ativo = false, data_atualizacao = CURRENT_TIMESTAMP WHERE token_hash = :tokenHash")
    Mono<Integer> deactivateToken(@Param("tokenHash") String tokenHash);
    
    /**
     * Remove tokens expirados fisicamente
     */
    @Query("DELETE FROM refresh_tokens WHERE data_expiracao < :dataLimite")
    Mono<Integer> deleteExpiredTokens(@Param("dataLimite") LocalDateTime dataLimite);
    
    /**
     * Remove tokens revogados antigos
     */
    @Query("DELETE FROM refresh_tokens WHERE revogado = true AND data_atualizacao < :dataLimite")
    Mono<Integer> deleteOldRevokedTokens(@Param("dataLimite") LocalDateTime dataLimite);
    
    /**
     * Conta tokens ativos de um usuário
     */
    @Query("SELECT COUNT(*) FROM refresh_tokens WHERE usuario_id = :usuarioId AND ativo = true AND revogado = false")
    Mono<Long> countActiveTokensByUsuario(@Param("usuarioId") Long usuarioId);
    
    /**
     * Conta tokens válidos (não expirados) de um usuário
     */
    @Query("SELECT COUNT(*) FROM refresh_tokens WHERE usuario_id = :usuarioId AND ativo = true AND revogado = false AND data_expiracao > CURRENT_TIMESTAMP")
    Mono<Long> countValidTokensByUsuario(@Param("usuarioId") Long usuarioId);
    
    /**
     * Busca tokens suspeitos (muitos tokens do mesmo IP)
     */
    @Query("SELECT rt.* FROM refresh_tokens rt WHERE rt.ip_origem = :ip AND rt.data_criacao > :dataLimite AND rt.ativo = true GROUP BY rt.ip_origem HAVING COUNT(*) > :limite")
    Flux<RefreshToken> findSuspiciousTokensByIP(@Param("ip") String ip, @Param("dataLimite") LocalDateTime dataLimite, @Param("limite") int limite);
    
    /**
     * Limpa tokens antigos de um usuário (mantém apenas os N mais recentes)
     */
    @Query("DELETE FROM refresh_tokens WHERE usuario_id = :usuarioId AND id NOT IN (SELECT id FROM refresh_tokens WHERE usuario_id = :usuarioId ORDER BY data_criacao DESC LIMIT :limite)")
    Mono<Integer> cleanupOldTokens(@Param("usuarioId") Long usuarioId, @Param("limite") int limite);
    
    /**
     * Busca estatísticas de uso por usuário
     */
    @Query("SELECT usuario_id, COUNT(*) as total, COUNT(CASE WHEN ativo = true THEN 1 END) as ativos FROM refresh_tokens WHERE usuario_id = :usuarioId GROUP BY usuario_id")
    Mono<Object> getTokenStatsByUsuario(@Param("usuarioId") Long usuarioId);
}