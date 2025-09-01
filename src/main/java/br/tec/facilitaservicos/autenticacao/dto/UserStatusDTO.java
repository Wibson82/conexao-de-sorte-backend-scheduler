package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO para status do usuário.
 */
@Schema(description = "Status do usuário")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserStatusDTO {
    
    @Schema(description = "ID do usuário")
    @JsonProperty("user_id")
    private Long userId;
    
    @Schema(description = "Status do usuário")
    @JsonProperty("status")
    private String status;
    
    @Schema(description = "Indica se o usuário está ativo")
    @JsonProperty("active")
    private Boolean active;
    
    @Schema(description = "Indica se a conta está bloqueada")
    @JsonProperty("blocked")
    private Boolean blocked;
    
    @Schema(description = "Indica se o email foi verificado")
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    
    // Construtor padrão
    public UserStatusDTO() {}
    
    // Construtor completo
    public UserStatusDTO(Long userId, String status, Boolean active, Boolean blocked, Boolean emailVerified) {
        this.userId = userId;
        this.status = status;
        this.active = active;
        this.blocked = blocked;
        this.emailVerified = emailVerified;
    }
    
    // Construtor simples
    public UserStatusDTO(Long userId, String status) {
        this.userId = userId;
        this.status = status;
    }
    
    // Getters e Setters
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public Boolean getBlocked() {
        return blocked;
    }
    
    public void setBlocked(Boolean blocked) {
        this.blocked = blocked;
    }
    
    public Boolean getEmailVerified() {
        return emailVerified;
    }
    
    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}