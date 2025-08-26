package br.tec.facilitaservicos.autenticacao.repository;

import br.tec.facilitaservicos.autenticacao.entity.Usuario;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Repositório R2DBC para entidade Usuario.
 * Todos os métodos retornam Mono/Flux para programação reativa.
 */
@Repository
public interface UsuarioRepository extends R2dbcRepository<Usuario, Long> {
    
    /**
     * Busca usuário por email
     */
    Mono<Usuario> findByEmail(String email);
    
    /**
     * Busca usuário por nome de usuário
     */
    Mono<Usuario> findByNomeUsuario(String nomeUsuario);
    
    /**
     * Busca usuário por email ou nome de usuário (para login)
     */
    @Query("SELECT * FROM usuarios WHERE (email = :identificador OR nome_usuario = :identificador) AND ativo = true")
    Mono<Usuario> findByEmailOrNomeUsuario(@Param("identificador") String identificador);
    
    /**
     * Busca usuários ativos
     */
    Flux<Usuario> findByAtivoTrue();
    
    /**
     * Busca usuários bloqueados
     */
    Flux<Usuario> findByContaBloqueadaTrue();
    
    /**
     * Busca usuários com email não verificado
     */
    Flux<Usuario> findByEmailVerificadoFalse();
    
    /**
     * Busca usuários bloqueados temporariamente (últimas 24h)
     */
    @Query("SELECT * FROM usuarios WHERE conta_bloqueada = true AND data_bloqueio > :dataLimite")
    Flux<Usuario> findUsuariosBloqueadosRecentemente(@Param("dataLimite") LocalDateTime dataLimite);
    
    /**
     * Conta usuários ativos
     */
    @Query("SELECT COUNT(*) FROM usuarios WHERE ativo = true")
    Mono<Long> countUsuariosAtivos();
    
    /**
     * Conta usuários bloqueados
     */
    @Query("SELECT COUNT(*) FROM usuarios WHERE conta_bloqueada = true")
    Mono<Long> countUsuariosBloqueados();
    
    /**
     * Busca usuários criados após uma data específica
     */
    Flux<Usuario> findByDataCriacaoAfter(LocalDateTime data);
    
    /**
     * Busca usuários que fizeram login recentemente (baseado em atualização)
     */
    @Query("SELECT * FROM usuarios WHERE data_atualizacao > :dataLimite AND tentativas_login_falidas = 0 AND ativo = true")
    Flux<Usuario> findUsuariosComLoginRecente(@Param("dataLimite") LocalDateTime dataLimite);
    
    /**
     * Atualiza tentativas de login falidas
     */
    @Query("UPDATE usuarios SET tentativas_login_falidas = :tentativas, data_atualizacao = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateTentativasLoginFalidas(@Param("id") Long id, @Param("tentativas") int tentativas);
    
    /**
     * Bloqueia conta do usuário
     */
    @Query("UPDATE usuarios SET conta_bloqueada = true, data_bloqueio = CURRENT_TIMESTAMP, data_atualizacao = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> bloquearConta(@Param("id") Long id);
    
    /**
     * Desbloqueia conta do usuário
     */
    @Query("UPDATE usuarios SET conta_bloqueada = false, data_bloqueio = NULL, tentativas_login_falidas = 0, data_atualizacao = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> desbloquearConta(@Param("id") Long id);
    
    /**
     * Verifica se email já existe
     */
    Mono<Boolean> existsByEmail(String email);
    
    /**
     * Verifica se nome de usuário já existe
     */
    Mono<Boolean> existsByNomeUsuario(String nomeUsuario);
    
    /**
     * Atualiza data de último acesso
     */
    @Query("UPDATE usuarios SET data_atualizacao = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> updateUltimoAcesso(@Param("id") Long id);
    
    /**
     * Busca usuários para limpeza (inativos há mais de X dias)
     */
    @Query("SELECT * FROM usuarios WHERE ativo = false AND data_atualizacao < :dataLimite")
    Flux<Usuario> findUsuariosParaLimpeza(@Param("dataLimite") LocalDateTime dataLimite);
    
    /**
     * Remove usuários inativos antigos (soft delete)
     */
    @Query("UPDATE usuarios SET ativo = false, email = CONCAT('deleted_', id, '_', email), data_atualizacao = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Integer> softDeleteUsuario(@Param("id") Long id);
}