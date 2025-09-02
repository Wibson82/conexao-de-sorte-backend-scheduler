package br.tec.facilitaservicos.autenticacao.dto;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para informações de usuário no contexto de autenticação.
 * 
 * Utilizado internamente pelo microserviço de autenticação para
 * gerenciar dados de usuários durante processos de login, autorização
 * e validação de sessões.
 * 
 * Principais casos de uso:
 * - Autenticação e autorização de usuários
 * - Controle de tentativas de login e bloqueios
 * - Validação de permissões e roles
 * - Rastreamento de atividade de login
 * 
 * Restrições de segurança:
 * - Campo password nunca deve ser exposto em respostas de API
 * - Informações de bloqueio são críticas para segurança
 * - Tentativas de login falhadas são monitoradas para detecção de ataques
 * 
 * Relacionamentos:
 * - Integra com o microserviço de usuários para dados básicos
 * - Conecta com sistema de auditoria para logs de acesso
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Informações de usuário para processo de autenticação e autorização")
public class UsuarioDTO {
    
    @Schema(description = "Identificador único do usuário", 
            example = "12345", 
            requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    
    @Schema(description = "Email do usuário usado como identificador principal", 
            example = "joao.silva@exemplo.com")
    private String email;
    
    @Schema(description = "Nome de usuário alternativo para login", 
            example = "joao.silva")
    private String username;
    
    @Schema(description = "Senha criptografada do usuário", 
            hidden = true,
            accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password; // Apenas para uso interno, nunca expor
    
    @Schema(description = "Primeiro nome do usuário", 
            example = "João")
    private String primeiroNome;
    
    @Schema(description = "Sobrenome do usuário", 
            example = "Silva")
    private String sobrenome;
    
    @Schema(description = "Conjunto de papéis/roles atribuídos ao usuário", 
            example = "[\"USER\", \"MODERATOR\", \"ADMIN\"]")
    private Set<String> roles;
    
    @Schema(description = "Conjunto de permissões específicas do usuário", 
            example = "[\"READ_MESSAGES\", \"WRITE_MESSAGES\", \"DELETE_OWN_MESSAGES\"]")
    private Set<String> permissoes;
    
    @Schema(description = "Indica se a conta do usuário está ativa", 
            example = "true")
    private boolean ativo;
    
    @Schema(description = "Indica se o email do usuário foi verificado", 
            example = "true")
    private boolean emailVerificado;
    
    @Schema(description = "Data e hora do último login realizado", 
            example = "2025-09-01T10:30:00")
    private LocalDateTime ultimoLogin;
    
    @Schema(description = "Número de tentativas consecutivas de login com falha", 
            example = "0",
            minimum = "0")
    private int tentativasLoginFalidas;
    
    @Schema(description = "Indica se a conta está temporariamente bloqueada", 
            example = "false")
    private boolean contaBloqueada;
    
    @Schema(description = "Data e hora em que a conta foi bloqueada", 
            example = "2025-09-01T15:45:00")
    private LocalDateTime dataBloqueio;
    
    @Schema(description = "Data e hora de criação da conta", 
            example = "2024-12-15T09:20:00")
    private LocalDateTime dataCriacao;
    
    @Schema(description = "Data e hora da última atualização dos dados", 
            example = "2025-08-30T16:30:00")
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