package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para requisição de login.
 * Record imutável seguindo padrões reativos.
 */
@Schema(description = "Dados para requisição de login")
public record RequisicaoLoginDTO(
    
    @Schema(description = "Nome de usuário ou email", example = "usuario@exemplo.com")
    @NotBlank(message = "Usuário é obrigatório")
    @Size(min = 3, max = 255, message = "Usuário deve ter entre 3 e 255 caracteres")
    @JsonProperty("username")
    String usuario,
    
    @Schema(description = "Senha do usuário", example = "senha123")
    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
    @JsonProperty("password")
    String senha
) {
    
    /**
     * Validação adicional no construtor compacto
     */
    public RequisicaoLoginDTO {
        // Trim dos valores de entrada
        usuario = usuario != null ? usuario.trim() : null;
        senha = senha != null ? senha.trim() : null;
        
        // Validações básicas adicionais
        if (usuario != null && usuario.isBlank()) {
            throw new IllegalArgumentException("Usuário não pode estar vazio");
        }
        if (senha != null && senha.isBlank()) {
            throw new IllegalArgumentException("Senha não pode estar vazia");
        }
    }
    
    /**
     * Método auxiliar para logs seguros (sem expor a senha)
     */
    public String toSecureString() {
        return String.format("RequisicaoLoginDTO{usuario='%s', senha='[PROTEGIDA]'}", usuario);
    }
    
    @Override
    public String toString() {
        return toSecureString();
    }
}