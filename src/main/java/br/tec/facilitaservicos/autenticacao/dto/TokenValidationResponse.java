package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO para resposta de validação de token para serviços.
 */
@Schema(description = "Resposta de validação de token")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenValidationResponse {
    
    @Schema(description = "Indica se o token é válido")
    @JsonProperty("valid")
    private boolean valid;
    
    @Schema(description = "ID do usuário")
    @JsonProperty("user_id")
    private Long userId;
    
    @Schema(description = "Nome de usuário")
    @JsonProperty("username")
    private String username;
    
    @Schema(description = "Email do usuário")
    @JsonProperty("email")
    private String email;
    
    @Schema(description = "Roles do usuário")
    @JsonProperty("roles")
    private Set<String> roles;
    
    @Schema(description = "Permissões do usuário")
    @JsonProperty("permissions")
    private Set<String> permissions;
    
    @Schema(description = "Data de expiração")
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Subject do token")
    @JsonProperty("subject")
    private String subject;
    
    @Schema(description = "Emissor do token")
    @JsonProperty("issuer")
    private String issuer;
    
    @Schema(description = "Mensagem de erro")
    @JsonProperty("error_message")
    private String errorMessage;
    
    // Construtor padrão
    public TokenValidationResponse() {}
    
    // Construtor para token válido
    public TokenValidationResponse(Long userId, String username, String email, 
                                 Set<String> roles, Set<String> permissions,
                                 LocalDateTime expiresAt, String subject, String issuer) {
        this.valid = true;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.permissions = permissions;
        this.expiresAt = expiresAt;
        this.subject = subject;
        this.issuer = issuer;
    }
    
    // Construtor para token inválido
    public TokenValidationResponse(String errorMessage) {
        this.valid = false;
        this.errorMessage = errorMessage;
    }
    
    // Getters e Setters
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Set<String> getRoles() {
        return roles;
    }
    
    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }
    
    public Set<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    public String getIssuer() {
        return issuer;
    }
    
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}