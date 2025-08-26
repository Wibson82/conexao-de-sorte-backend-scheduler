package br.tec.facilitaservicos.autenticacao.entity;

import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade RefreshToken para R2DBC.
 * Representa um token de renovação no sistema.
 */
@Table("refresh_tokens")
public class RefreshToken {
    
    @Id
    @Column("id")
    private Long id;
    
    @Column("token_hash")
    private String tokenHash;
    
    @Column("usuario_id")
    private Long usuarioId;
    
    @Column("ativo")
    private boolean ativo = true;
    
    @Column("revogado")
    private boolean revogado = false;
    
    @Column("data_expiracao")
    private LocalDateTime dataExpiracao;
    
    @Column("ip_origem")
    private String ipOrigem;
    
    @Column("user_agent")
    private String userAgent;
    
    @Column("familia_token")
    private String familiaToken;
    
    @CreatedDate
    @Column("data_criacao")
    private LocalDateTime dataCriacao;
    
    @LastModifiedDate
    @Column("data_atualizacao")
    private LocalDateTime dataAtualizacao;
    
    @Version
    @Column("versao")
    private Long versao;
    
    // Construtores
    public RefreshToken() {
        this.familiaToken = UUID.randomUUID().toString();
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    public RefreshToken(String tokenHash, Long usuarioId, LocalDateTime dataExpiracao) {
        this();
        this.tokenHash = tokenHash;
        this.usuarioId = usuarioId;
        this.dataExpiracao = dataExpiracao;
    }
    
    public RefreshToken(String tokenHash, Long usuarioId, LocalDateTime dataExpiracao, 
                       String ipOrigem, String userAgent) {
        this(tokenHash, usuarioId, dataExpiracao);
        this.ipOrigem = ipOrigem;
        this.userAgent = userAgent;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTokenHash() {
        return tokenHash;
    }
    
    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }
    
    public Long getUsuarioId() {
        return usuarioId;
    }
    
    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }
    
    public boolean isAtivo() {
        return ativo;
    }
    
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
    
    public boolean isRevogado() {
        return revogado;
    }
    
    public void setRevogado(boolean revogado) {
        this.revogado = revogado;
    }
    
    public LocalDateTime getDataExpiracao() {
        return dataExpiracao;
    }
    
    public void setDataExpiracao(LocalDateTime dataExpiracao) {
        this.dataExpiracao = dataExpiracao;
    }
    
    public String getIpOrigem() {
        return ipOrigem;
    }
    
    public void setIpOrigem(String ipOrigem) {
        this.ipOrigem = ipOrigem;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    
    public String getFamiliaToken() {
        return familiaToken;
    }
    
    public void setFamiliaToken(String familiaToken) {
        this.familiaToken = familiaToken;
    }
    
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }
    
    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }
    
    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
    
    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
    
    public Long getVersao() {
        return versao;
    }
    
    public void setVersao(Long versao) {
        this.versao = versao;
    }
    
    // Métodos de negócio
    
    /**
     * Verifica se o token está expirado
     */
    public boolean isExpirado() {
        return dataExpiracao != null && LocalDateTime.now().isAfter(dataExpiracao);
    }
    
    /**
     * Verifica se o token está válido
     */
    public boolean isValido() {
        return ativo && !revogado && !isExpirado();
    }
    
    /**
     * Revoga o token
     */
    public void revogar() {
        this.revogado = true;
        this.ativo = false;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Desativa o token sem revogar (para rotação)
     */
    public void desativar() {
        this.ativo = false;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Calcula tempo restante em segundos até expiração
     */
    public long getTempoRestanteSegundos() {
        if (dataExpiracao == null || isExpirado()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), dataExpiracao).getSeconds();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(id, that.id) &&
               Objects.equals(tokenHash, that.tokenHash);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, tokenHash);
    }
    
    @Override
    public String toString() {
        return String.format(
            "RefreshToken{id=%d, usuarioId=%d, ativo=%s, revogado=%s, expirado=%s, familiaToken='%s'}",
            id, usuarioId, ativo, revogado, isExpirado(), familiaToken
        );
    }
}