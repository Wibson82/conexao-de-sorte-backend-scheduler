package br.tec.facilitaservicos.autenticacao.dto;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsuarioDTO {
    private Long id;
    private String email;
    private String username;
    private String password; // Only for internal use, never expose
    private String primeiroNome;
    private String sobrenome;
    private Set<String> roles;
    private Set<String> permissoes;
    private boolean ativo;
    private boolean emailVerificado;
    private LocalDateTime ultimoLogin;
    private int tentativasLoginFalidas;
    private boolean contaBloqueada;
    private LocalDateTime dataBloqueio;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;

    // Construtor padrão (NoArgsConstructor)
    public UsuarioDTO() {
    }

    // Construtor completo (AllArgsConstructor)
    public UsuarioDTO(Long id, String email, String username, String password, String primeiroNome, String sobrenome,
                      Set<String> roles, Set<String> permissoes, boolean ativo, boolean emailVerificado,
                      LocalDateTime ultimoLogin, int tentativasLoginFalidas, boolean contaBloqueada,
                      LocalDateTime dataBloqueio, LocalDateTime dataCriacao, LocalDateTime dataAtualizacao) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.primeiroNome = primeiroNome;
        this.sobrenome = sobrenome;
        this.roles = roles;
        this.permissoes = permissoes;
        this.ativo = ativo;
        this.emailVerificado = emailVerificado;
        this.ultimoLogin = ultimoLogin;
        this.tentativasLoginFalidas = tentativasLoginFalidas;
        this.contaBloqueada = contaBloqueada;
        this.dataBloqueio = dataBloqueio;
        this.dataCriacao = dataCriacao;
        this.dataAtualizacao = dataAtualizacao;
    }

    // Construtor para uso interno do AuthService (mantido do original)
    public UsuarioDTO(Long id, String email, String username, String password, Set<String> roles, Set<String> permissoes, boolean ativo, boolean emailVerificado, LocalDateTime ultimoLogin, int tentativasLoginFalidas, boolean contaBloqueada, LocalDateTime dataBloqueio) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.permissoes = permissoes;
        this.ativo = ativo;
        this.emailVerificado = emailVerificado;
        this.ultimoLogin = ultimoLogin;
        this.tentativasLoginFalidas = tentativasLoginFalidas;
        this.contaBloqueada = contaBloqueada;
        this.dataBloqueio = dataBloqueio;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPrimeiroNome() {
        return primeiroNome;
    }

    public void setPrimeiroNome(String primeiroNome) {
        this.primeiroNome = primeiroNome;
    }

    public String getSobrenome() {
        return sobrenome;
    }

    public void setSobrenome(String sobrenome) {
        this.sobrenome = sobrenome;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(Set<String> permissoes) {
        this.permissoes = permissoes;
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

    public LocalDateTime getUltimoLogin() {
        return ultimoLogin;
    }

    public void setUltimoLogin(LocalDateTime ultimoLogin) {
        this.ultimoLogin = ultimoLogin;
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

    // Métodos auxiliares para compatibilidade com testes
    public String getFullName() {
        if (primeiroNome != null && sobrenome != null) {
            return primeiroNome + " " + sobrenome;
        } else if (primeiroNome != null) {
            return primeiroNome;
        } else if (sobrenome != null) {
            return sobrenome;
        }
        return username != null ? username : email;
    }

    public Boolean getActive() {
        return ativo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UsuarioDTO that = (UsuarioDTO) o;
        return ativo == that.ativo &&
               emailVerificado == that.emailVerificado &&
               tentativasLoginFalidas == that.tentativasLoginFalidas &&
               contaBloqueada == that.contaBloqueada &&
               Objects.equals(id, that.id) &&
               Objects.equals(email, that.email) &&
               Objects.equals(username, that.username) &&
               Objects.equals(password, that.password) &&
               Objects.equals(primeiroNome, that.primeiroNome) &&
               Objects.equals(sobrenome, that.sobrenome) &&
               Objects.equals(roles, that.roles) &&
               Objects.equals(permissoes, that.permissoes) &&
               Objects.equals(ultimoLogin, that.ultimoLogin) &&
               Objects.equals(dataBloqueio, that.dataBloqueio) &&
               Objects.equals(dataCriacao, that.dataCriacao) &&
               Objects.equals(dataAtualizacao, that.dataAtualizacao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email, username, password, primeiroNome, sobrenome, roles, permissoes, ativo,
                            emailVerificado, ultimoLogin, tentativasLoginFalidas, contaBloqueada, dataBloqueio,
                            dataCriacao, dataAtualizacao);
    }

    @Override
    public String toString() {
        return "UsuarioDTO{" +
               "id=" + id +
               ", email='" + email + "'" +
               ", username='" + username + "'" +
               ", primeiroNome='" + primeiroNome + "'" +
               ", sobrenome='" + sobrenome + "'" +
               ", roles=" + roles +
               ", permissoes=" + permissoes +
               ", ativo=" + ativo +
               ", emailVerificado=" + emailVerificado +
               ", ultimoLogin=" + ultimoLogin +
               ", tentativasLoginFalidas=" + tentativasLoginFalidas +
               ", contaBloqueada=" + contaBloqueada +
               ", dataBloqueio=" + dataBloqueio +
               ", dataCriacao=" + dataCriacao +
               ", dataAtualizacao=" + dataAtualizacao +
               '}';
    }
}