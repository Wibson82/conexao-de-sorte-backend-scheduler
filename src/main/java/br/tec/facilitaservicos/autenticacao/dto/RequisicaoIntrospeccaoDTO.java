package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para requisição de introspecção de token (RFC 7662).
 * Record imutável seguindo padrões reativos.
 */
@Schema(description = "Dados para introspecção de token")
public record RequisicaoIntrospeccaoDTO(
    
    @Schema(description = "Token para verificação", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "Token é obrigatório")
    @JsonProperty("token")
    String token,
    
    @Schema(description = "Dica do tipo de token", example = "access_token", allowableValues = {"access_token", "refresh_token"})
    @JsonProperty("token_type_hint")
    String tipoTokenHint
) {
    
    /**
     * Validação no construtor compacto
     */
    public RequisicaoIntrospeccaoDTO {
        // Trim dos valores de entrada
        token = token != null ? token.trim() : null;
        tipoTokenHint = tipoTokenHint != null ? tipoTokenHint.trim() : null;
        
        // Validação básica
        if (token != null && token.isBlank()) {
            throw new IllegalArgumentException("Token não pode estar vazio");
        }
        
        // Valores padrão
        tipoTokenHint = tipoTokenHint != null && !tipoTokenHint.isBlank() ? tipoTokenHint : "access_token";
    }
    
    /**
     * Método auxiliar para logs seguros (token mascarado)
     */
    public String getTokenMascarado() {
        if (token == null || token.length() < 10) {
            return "[INVÁLIDO]";
        }
        return token.substring(0, 5) + "..." + token.substring(token.length() - 5);
    }
    
    /**
     * String segura para logs
     */
    public String toSecureString() {
        return String.format("RequisicaoIntrospeccaoDTO{token='%s', tipoTokenHint='%s'}", 
                           getTokenMascarado(), tipoTokenHint);
    }
    
    @Override
    public String toString() {
        return toSecureString();
    }
}