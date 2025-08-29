package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import br.tec.facilitaservicos.autenticacao.entity.Usuario;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * DTO para informações básicas do usuário.
 * 
 * Usado para comunicação inter-service, contendo apenas
 * informações não-sensíveis necessárias para outros microserviços.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Informações básicas do usuário")
public class UsuarioDTO {
    
    @Schema(description = "ID único do usuário", example = "123")
    private Long id;
    
    @Schema(description = "Nome de usuário/email", example = "usuario@exemplo.com")
    private String username;
    
    @Schema(description = "Nome completo", example = "João Silva")
    private String fullName;
    
    @Schema(description = "Email do usuário", example = "usuario@exemplo.com")
    private String email;
    
    @Schema(description = "Primeiro nome", example = "João")
    private String primeiroNome;
    
    @Schema(description = "Sobrenome", example = "Silva")
    private String sobrenome;
    
    @Schema(description = "Status ativo", example = "true")
    private Boolean active;
    
    @Schema(description = "Email verificado", example = "true")
    private Boolean emailVerified;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Data de criação", example = "2024-01-15T10:30:00Z")
    private Instant criadoEm;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Último login", example = "2024-01-16T14:22:10Z")
    private Instant ultimoLogin;
    
    @Schema(description = "Roles do usuário", example = "[\"USER\", \"ADMIN\"]")
    private List<String> roles;
    
    @Schema(description = "Permissões específicas", example = "[\"READ\", \"WRITE\"]")
    private List<String> permissoes;
    
    @Schema(description = "URL do avatar", example = "https://example.com/avatar.jpg")
    private String avatarUrl;
    
    @Schema(description = "Timezone", example = "America/Sao_Paulo")
    private String timezone;
    
    @Schema(description = "Locale", example = "pt_BR")
    private String locale;
    
    // Constructors
    public UsuarioDTO() {}
    
    public UsuarioDTO(Long id, String username, String fullName, String email, 
                      String primeiroNome, String sobrenome, Boolean active, Boolean emailVerified,
                      Instant criadoEm, Instant ultimoLogin, List<String> roles, List<String> permissoes,
                      String avatarUrl, String timezone, String locale) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.primeiroNome = primeiroNome;
        this.sobrenome = sobrenome;
        this.active = active;
        this.emailVerified = emailVerified;
        this.criadoEm = criadoEm;
        this.ultimoLogin = ultimoLogin;
        this.roles = roles;
        this.permissoes = permissoes;
        this.avatarUrl = avatarUrl;
        this.timezone = timezone;
        this.locale = locale;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPrimeiroNome() { return primeiroNome; }
    public void setPrimeiroNome(String primeiroNome) { this.primeiroNome = primeiroNome; }
    
    public String getSobrenome() { return sobrenome; }
    public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public Instant getCriadoEm() { return criadoEm; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    
    public Instant getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(Instant ultimoLogin) { this.ultimoLogin = ultimoLogin; }
    
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    
    public List<String> getPermissoes() { return permissoes; }
    public void setPermissoes(List<String> permissoes) { this.permissoes = permissoes; }
    
    public List<String> getPermissions() { return permissoes; }
    public void setPermissions(List<String> permissions) { this.permissoes = permissions; }
    
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    
    // Builder Pattern - Manual Implementation (NO LOMBOK)
    public static UsuarioDTOBuilder builder() {
        return new UsuarioDTOBuilder();
    }
    
    public static class UsuarioDTOBuilder {
        private Long id;
        private String username;
        private String fullName;
        private String email;
        private String primeiroNome;
        private String sobrenome;
        private Boolean active;
        private Boolean emailVerified;
        private Instant criadoEm;
        private Instant ultimoLogin;
        private List<String> roles;
        private List<String> permissoes;
        private String avatarUrl;
        private String timezone;
        private String locale;
        
        public UsuarioDTOBuilder id(Long id) { this.id = id; return this; }
        public UsuarioDTOBuilder username(String username) { this.username = username; return this; }
        public UsuarioDTOBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public UsuarioDTOBuilder email(String email) { this.email = email; return this; }
        public UsuarioDTOBuilder primeiroNome(String primeiroNome) { this.primeiroNome = primeiroNome; return this; }
        public UsuarioDTOBuilder sobrenome(String sobrenome) { this.sobrenome = sobrenome; return this; }
        public UsuarioDTOBuilder active(Boolean active) { this.active = active; return this; }
        public UsuarioDTOBuilder emailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; return this; }
        public UsuarioDTOBuilder criadoEm(Instant criadoEm) { this.criadoEm = criadoEm; return this; }
        public UsuarioDTOBuilder ultimoLogin(Instant ultimoLogin) { this.ultimoLogin = ultimoLogin; return this; }
        public UsuarioDTOBuilder roles(List<String> roles) { this.roles = roles; return this; }
        public UsuarioDTOBuilder permissoes(List<String> permissoes) { this.permissoes = permissoes; return this; }
        public UsuarioDTOBuilder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public UsuarioDTOBuilder timezone(String timezone) { this.timezone = timezone; return this; }
        public UsuarioDTOBuilder locale(String locale) { this.locale = locale; return this; }
        
        public UsuarioDTO build() {
            return new UsuarioDTO(id, username, fullName, email, primeiroNome, sobrenome, 
                                 active, emailVerified, criadoEm, ultimoLogin, roles, permissoes,
                                 avatarUrl, timezone, locale);
        }
    }
    
    // Factory methods from Entity
    public static UsuarioDTO from(Usuario usuario) {
        if (usuario == null) return null;
        
        return UsuarioDTO.builder()
            .id(usuario.getId())
            .username(usuario.getUsername())
            .email(usuario.getEmail())
            .fullName(usuario.getNomeCompleto())
            .primeiroNome(usuario.getPrimeiroNome())
            .sobrenome(usuario.getSobrenome())
            .active(usuario.isAtivo())
            .emailVerified(usuario.isEmailVerificado())
            .criadoEm(usuario.getCriadoEm() != null ? usuario.getCriadoEm().toInstant(ZoneOffset.UTC) : null)
            .ultimoLogin(usuario.getUltimoLogin() != null ? usuario.getUltimoLogin().toInstant(ZoneOffset.UTC) : null)
            .roles(usuario.getRoles() != null ? List.of(usuario.getRoles().split(",")) : List.of())
            .permissoes(usuario.getPermissoes() != null ? List.of(usuario.getPermissoes().split(",")) : List.of())
            .avatarUrl(usuario.getAvatarUrl())
            .timezone(usuario.getTimezone())
            .locale(usuario.getLocale())
            .build();
    }
    
    public static UsuarioDTO basicInfo(Usuario usuario) {
        if (usuario == null) return null;
        
        return UsuarioDTO.builder()
            .id(usuario.getId())
            .username(usuario.getUsername())
            .email(usuario.getEmail())
            .fullName(usuario.getNomeCompleto())
            .active(usuario.isAtivo())
            .build();
    }
    
    public static UsuarioDTO anonimo(Long userId) {
        return UsuarioDTO.builder()
            .id(userId)
            .username("Usuário Anônimo")
            .fullName("Usuário Anônimo")
            .active(true)
            .build();
    }
    
    public String toLogString() {
        return String.format("Usuario[id=%s, username=%s, email=%s, active=%s]", 
                           id, username, email, active);
    }
}