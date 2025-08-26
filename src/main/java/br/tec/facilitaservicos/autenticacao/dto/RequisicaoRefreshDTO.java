package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para requisição de renovação de token.
 * Record imutável seguindo padrões reativos.
 */
@Schema(description = "Dados para renovação de token")
public record RequisicaoRefreshDTO(
    
    @Schema(description = "Token de renovação", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank(message = "Token de renovação é obrigatório")
    @JsonProperty("refresh_token")
    String tokenRenovacao
) {
    
    /**
     * Validação no construtor compacto
     */
    public RequisicaoRefreshDTO {
        // Trim do valor de entrada
        tokenRenovacao = tokenRenovacao != null ? tokenRenovacao.trim() : null;
        
        // Validação básica
        if (tokenRenovacao != null && tokenRenovacao.isBlank()) {
            throw new IllegalArgumentException("Token de renovação não pode estar vazio");
        }
    }
    
    /**
     * Método auxiliar para logs seguros (mascarado)
     */
    public String getTokenRenovacaoMascarado() {
        if (tokenRenovacao == null || tokenRenovacao.length() < 10) {
            return "[INVÁLIDO]";
        }
        return tokenRenovacao.substring(0, 5) + "..." + tokenRenovacao.substring(tokenRenovacao.length() - 5);
    }
    
    /**
     * String segura para logs
     */
    public String toSecureString() {
        return String.format("RequisicaoRefreshDTO{tokenRenovacao='%s'}", getTokenRenovacaoMascarado());
    }
    
    @Override
    public String toString() {
        return toSecureString();
    }
}