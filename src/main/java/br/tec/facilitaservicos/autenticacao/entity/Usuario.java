package br.tec.facilitaservicos.autenticacao.entity;

import org.springframework.data.annotation.*;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidade Usuario adaptada para R2DBC.
 * Representa um usuário no sistema de autenticação.
 */
@Table("usuarios")
public class Usuario {
    
    @Id
    @Column("id")
    private Long id;
    
    @Column("email")
    private String email;
    
    @Column("nome_usuario")
    private String nomeUsuario;
    
    @Column("senha_hash")
    private String senhaHash;
    
    @Column("ativo")
    private boolean ativo = true;
    
    @Column("email_verificado")
    private boolean emailVerificado = false;
    
    @Column("tentativas_login_falidas")
    private int tentativasLoginFalidas = 0;
    
    @Column("conta_bloqueada")
    private boolean contaBloqueada = false;
    
    @Column("data_bloqueio")
    private LocalDateTime dataBloqueio;
    
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
    public Usuario() {
        this.dataCriacao = LocalDateTime.now();
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    public Usuario(String email, String nomeUsuario, String senhaHash) {
        this();
        this.email = email;
        this.nomeUsuario = nomeUsuario;
        this.senhaHash = senhaHash;
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getNomeUsuario() {
        return nomeUsuario;
    }
    
    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }
    
    public String getSenhaHash() {
        return senhaHash;
    }
    
    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }
    
    public boolean isAtivo() {
        return ativo;
    }
    
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
    
    public boolean isEmailVerificado() {
        return emailVerificado;
    }
    
    public void setEmailVerificado(boolean emailVerificado) {
        this.emailVerificado = emailVerificado;
    }
    
    public int getTentativasLoginFalidas() {
        return tentativasLoginFalidas;
    }
    
    public void setTentativasLoginFalidas(int tentativasLoginFalidas) {
        this.tentativasLoginFalidas = tentativasLoginFalidas;
    }
    
    public boolean isContaBloqueada() {
        return contaBloqueada;
    }
    
    public void setContaBloqueada(boolean contaBloqueada) {
        this.contaBloqueada = contaBloqueada;
    }
    
    public LocalDateTime getDataBloqueio() {
        return dataBloqueio;
    }
    
    public void setDataBloqueio(LocalDateTime dataBloqueio) {
        this.dataBloqueio = dataBloqueio;
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
     * Incrementa tentativas de login falhadas
     */
    public void incrementarTentativasFalidas() {
        this.tentativasLoginFalidas++;
        this.dataAtualizacao = LocalDateTime.now();
        
        // Bloqueia conta após 5 tentativas falidas
        if (this.tentativasLoginFalidas >= 5) {
            this.contaBloqueada = true;
            this.dataBloqueio = LocalDateTime.now();
        }
    }
    
    /**
     * Reseta tentativas de login após sucesso
     */
    public void resetarTentativasFalidas() {
        this.tentativasLoginFalidas = 0;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Desbloqueia conta
     */
    public void desbloquearConta() {
        this.contaBloqueada = false;
        this.dataBloqueio = null;
        this.tentativasLoginFalidas = 0;
        this.dataAtualizacao = LocalDateTime.now();
    }
    
    /**
     * Verifica se o usuário pode fazer login
     */
    public boolean podeLogar() {
        return ativo && !contaBloqueada && emailVerificado;
    }
    
    /**
     * Verifica se a conta está temporariamente bloqueada (menos de 30 min)
     */
    public boolean isBloqueioTemporario() {
        if (!contaBloqueada || dataBloqueio == null) {
            return false;
        }
        return dataBloqueio.isAfter(LocalDateTime.now().minusMinutes(30));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id) &&
               Objects.equals(email, usuario.email);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }
    
    @Override
    public String toString() {
        return String.format(
            "Usuario{id=%d, email='%s', nomeUsuario='%s', ativo=%s, emailVerificado=%s, contaBloqueada=%s}",
            id, email, nomeUsuario, ativo, emailVerificado, contaBloqueada
        );
    }
}