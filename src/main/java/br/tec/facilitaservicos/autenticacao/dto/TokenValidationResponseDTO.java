package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * DTO para resposta de validação de token JWT para comunicação inter-service.
 * 
 * Contém informações essenciais sobre a validade do token e dados básicos
 * do usuário que outros microserviços precisam para autorização.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta da validação de token JWT")
public class TokenValidationResponseDTO {
    
    @Schema(description = "Indica se o token é válido", example = "true")
    private boolean valid;
    
    @Schema(description = "ID do usuário proprietário do token", example = "123")
    private Long userId;
    
    @Schema(description = "Nome de usuário", example = "usuario@exemplo.com")
    private String username;
    
    @Schema(description = "Email do usuário", example = "usuario@exemplo.com")
    private String email;
    
    @Schema(description = "Roles/papéis do usuário", example = "[\"USER\", \"ADMIN\"]")
    private List<String> roles;
    
    @Schema(description = "Permissões específicas", example = "[\"read\", \"WRITE\"]")
    private List<String> permissions;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    @Schema(description = "Data de expiração do token", example = "2024-01-15T18:30:00Z")
    private Instant expiresAt;
    
    @Schema(description = "Subject do token", example = "usuario@exemplo.com")
    private String subject;
    
    @Schema(description = "Issuer do token", example = "conexao-de-sorte-auth")
    private String issuer;
    
    @Schema(description = "Mensagem de erro (quando inválido)", example = "Token expired")
    private String errorMessage;
    
    // Constructors
    public TokenValidationResponseDTO() {}
    
    public TokenValidationResponseDTO(boolean valid, Long userId, String username, String email,
                                     List<String> roles, List<String> permissions, Instant expiresAt,
                                     String subject, String issuer, String errorMessage) {
        this.valid = valid;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.roles = roles;
        this.permissions = permissions;
        this.expiresAt = expiresAt;
        this.subject = subject;
        this.issuer = issuer;
        this.errorMessage = errorMessage;
    }
    
    // Getters and Setters
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
    
    public List<String> getPermissions() { return permissions; }
    public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    // Builder Pattern - Manual Implementation (NO LOMBOK)
    public static TokenValidationResponseDTOBuilder builder() {
        return new TokenValidationResponseDTOBuilder();
    }
    
    public static class TokenValidationResponseDTOBuilder {
        private boolean valid;
        private Long userId;
        private String username;
        private String email;
        private List<String> roles;
        private List<String> permissions;
        private Instant expiresAt;
        private String subject;
        private String issuer;
        private String errorMessage;
        
        public TokenValidationResponseDTOBuilder valid(boolean valid) { this.valid = valid; return this; }
        public TokenValidationResponseDTOBuilder userId(Long userId) { this.userId = userId; return this; }
        public TokenValidationResponseDTOBuilder username(String username) { this.username = username; return this; }
        public TokenValidationResponseDTOBuilder email(String email) { this.email = email; return this; }
        public TokenValidationResponseDTOBuilder roles(List<String> roles) { this.roles = roles; return this; }
        public TokenValidationResponseDTOBuilder permissions(List<String> permissions) { this.permissions = permissions; return this; }
        public TokenValidationResponseDTOBuilder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public TokenValidationResponseDTOBuilder subject(String subject) { this.subject = subject; return this; }
        public TokenValidationResponseDTOBuilder issuer(String issuer) { this.issuer = issuer; return this; }
        public TokenValidationResponseDTOBuilder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        
        public TokenValidationResponseDTO build() {
            return new TokenValidationResponseDTO(valid, userId, username, email, roles, permissions,
                                                 expiresAt, subject, issuer, errorMessage);
        }
    }
    
    // Factory methods
    public static TokenValidationResponseDTO valid(Long userId, String username, String email, 
                                                  List<String> roles, List<String> permissions, 
                                                  Instant expiresAt, String subject, String issuer) {
        return builder()
                .valid(true)
                .userId(userId)
                .username(username)
                .email(email)
                .roles(roles)
                .permissions(permissions)
                .expiresAt(expiresAt)
                .subject(subject)
                .issuer(issuer)
                .build();
    }
    
    public static TokenValidationResponseDTO invalid(String errorMessage) {
        return builder()
                .valid(false)
                .errorMessage(errorMessage)
                .build();
    }
    
    public static TokenValidationResponseDTO expired() {
        return invalid("Token expired");
    }
    
    public static TokenValidationResponseDTO malformed() {
        return invalid("Token malformed");
    }
    
    public static TokenValidationResponseDTO userNotFound() {
        return invalid("User not found");
    }
}