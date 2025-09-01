package br.tec.facilitaservicos.autenticacao.dto;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Data Transfer Object for User information")
public class UsuarioDTO {
    @Schema(description = "Unique identifier of the user", example = "1")
    private Long id;
    @Schema(description = "Email of the user", example = "user@example.com")
    private String email;
    @Schema(description = "Username of the user", example = "user")
    private String username;
    @Schema(description = "Password of the user", hidden = true)
    private String password; // Only for internal use, never expose
    @Schema(description = "First name of the user", example = "John")
    private String primeiroNome;
    @Schema(description = "Last name of the user", example = "Doe")
    private String sobrenome;
    @Schema(description = "Set of roles assigned to the user", example = "[\"USER\", \"ADMIN\"]")
    private Set<String> roles;
    @Schema(description = "Set of permissions assigned to the user", example = "[\"READ\", \"WRITE\"]")
    private Set<String> permissoes;
    @Schema(description = "Indicates if the user is active", example = "true")
    private boolean ativo;
    @Schema(description = "Indicates if the user's email is verified", example = "true")
    private boolean emailVerificado;
    @Schema(description = "Last login date and time", example = "2025-01-01T12:00:00")
    private LocalDateTime ultimoLogin;
    @Schema(description = "Number of failed login attempts", example = "0")
    private int tentativasLoginFalidas;
    @Schema(description = "Indicates if the user's account is locked", example = "false")
    private boolean contaBloqueada;
    @Schema(description = "Date and time when the account was locked", example = "2025-01-01T12:00:00")
    private LocalDateTime dataBloqueio;
    @Schema(description = "Date and time of account creation", example = "2025-01-01T12:00:00")
    private LocalDateTime dataCriacao;
    @Schema(description = "Date and time of last account update", example = "2025-01-01T12:00:00")
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