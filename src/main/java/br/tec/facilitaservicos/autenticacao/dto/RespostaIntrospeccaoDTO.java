package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * DTO para resposta de introspecção de token (RFC 7662).
 * Record imutável seguindo padrões reativos.
 */
@Schema(description = "Resposta de introspecção de token")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespostaIntrospeccaoDTO(
    
    @Schema(description = "Indica se o token está ativo", example = "true")
    @JsonProperty("active")
    boolean ativo,
    
    @Schema(description = "Subject (usuário) do token", example = "user123")
    @JsonProperty("sub")
    String subject,
    
    @Schema(description = "Emissor do token", example = "https://auth.conexaodesorte.com")
    @JsonProperty("iss")
    String emissor,
    
    @Schema(description = "Audiência do token", example = "conexao-de-sorte")
    @JsonProperty("aud")
    String audiencia,
    
    @Schema(description = "Data/hora de expiração (Unix timestamp)")
    @JsonProperty("exp")
    Long expiracao,
    
    @Schema(description = "Data/hora de emissão (Unix timestamp)")
    @JsonProperty("iat")
    Long emissao,
    
    @Schema(description = "Escopo/permissões do token")
    @JsonProperty("scope")
    String escopo,
    
    @Schema(description = "ID do cliente")
    @JsonProperty("client_id")
    String clienteId,
    
    @Schema(description = "Tipo do token", example = "Bearer")
    @JsonProperty("token_type")
    String tipoToken,
    
    @Schema(description = "Nome de usuário")
    @JsonProperty("username")
    String nomeUsuario,
    
    @Schema(description = "Permissões do usuário")
    @JsonProperty("authorities")
    Set<String> autoridades
) {
    
    /**
     * Construtor compacto com valores padrão
     */
    public RespostaIntrospeccaoDTO {
        // Garantir imutabilidade das coleções
        if (autoridades != null) {
            autoridades = Set.copyOf(autoridades);
        }
    }
    
    /**
     * Factory method para token ativo
     */
    public static RespostaIntrospeccaoDTO ativo(String subject, String emissor, String audiencia,
                                               Long expiracao, Long emissao, Set<String> autoridades) {
        return new RespostaIntrospeccaoDTO(
            true,
            subject,
            emissor,
            audiencia,
            expiracao,
            emissao,
            autoridades != null ? String.join(" ", autoridades) : null,
            null,
            "Bearer",
            subject,
            autoridades
        );
    }
    
    /**
     * Factory method para token inativo
     */
    public static RespostaIntrospeccaoDTO inativo() {
        return new RespostaIntrospeccaoDTO(
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
    
    /**
     * Verifica se o token expirou baseado no timestamp
     */
    public boolean isExpirado() {
        if (expiracao == null) {
            return false;
        }
        return System.currentTimeMillis() / 1000 >= expiracao;
    }
    
    /**
     * Converte timestamp Unix para LocalDateTime
     */
    public LocalDateTime getDataExpiracao() {
        if (expiracao == null) {
            return null;
        }
        return LocalDateTime.ofEpochSecond(expiracao, 0, java.time.ZoneOffset.UTC);
    }
    
    /**
     * Converte timestamp Unix para LocalDateTime
     */
    public LocalDateTime getDataEmissao() {
        if (emissao == null) {
            return null;
        }
        return LocalDateTime.ofEpochSecond(emissao, 0, java.time.ZoneOffset.UTC);
    }
}