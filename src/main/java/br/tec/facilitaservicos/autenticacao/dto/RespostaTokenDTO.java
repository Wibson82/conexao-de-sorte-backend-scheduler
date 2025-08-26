package br.tec.facilitaservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * DTO para resposta de token de autenticação.
 * Record imutável otimizado para WebFlux.
 */
@Schema(description = "Resposta de autenticação com tokens")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RespostaTokenDTO(
    
    @Schema(description = "Token de acesso JWT", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    @JsonProperty("access_token")
    String tokenAcesso,
    
    @Schema(description = "Token de renovação", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @JsonProperty("refresh_token") 
    String tokenRenovacao,
    
    @Schema(description = "Tipo do token", example = "Bearer")
    @JsonProperty("token_type")
    String tipoToken,
    
    @Schema(description = "Tempo de validade em segundos", example = "3600")
    @JsonProperty("expires_in")
    Long tempoValidadeSegundos,
    
    @Schema(description = "Data e hora de expiração")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonProperty("expires_at")
    LocalDateTime dataExpiracao,
    
    @Schema(description = "Permissões do usuário")
    @JsonProperty("scope")
    Set<String> permissoes,
    
    @Schema(description = "ID da sessão")
    @JsonProperty("session_id")
    String idSessao,
    
    @Schema(description = "Informações adicionais")
    @JsonProperty("additional_info")
    Map<String, Object> informacoesAdicionais
) {
    
    /**
     * Construtor compacto com validações e valores padrão
     */
    public RespostaTokenDTO {
        // Valores padrão
        tipoToken = tipoToken != null ? tipoToken : "Bearer";
        tempoValidadeSegundos = tempoValidadeSegundos != null ? tempoValidadeSegundos : 3600L;
        
        // Validação básica de formato JWT para access token
        if (tokenAcesso != null && !tokenAcesso.isBlank()) {
            if (!tokenAcesso.matches("^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]*$")) {
                throw new IllegalArgumentException("Token de acesso não está em formato JWT válido");
            }
        }
        
        // Garantir imutabilidade das coleções
        if (permissoes != null) {
            permissoes = Set.copyOf(permissoes);
        }
        if (informacoesAdicionais != null) {
            informacoesAdicionais = Map.copyOf(informacoesAdicionais);
        }
    }
    
    /**
     * Factory method para criar resposta básica
     */
    public static RespostaTokenDTO of(String tokenAcesso, String tokenRenovacao, long tempoValidadeSegundos) {
        return new RespostaTokenDTO(
            tokenAcesso,
            tokenRenovacao,
            "Bearer",
            tempoValidadeSegundos,
            LocalDateTime.now().plusSeconds(tempoValidadeSegundos),
            null,
            null,
            null
        );
    }
    
    /**
     * Factory method para criar resposta com permissões
     */
    public static RespostaTokenDTO of(String tokenAcesso, String tokenRenovacao, 
                                     long tempoValidadeSegundos, Set<String> permissoes) {
        return new RespostaTokenDTO(
            tokenAcesso,
            tokenRenovacao,
            "Bearer",
            tempoValidadeSegundos,
            LocalDateTime.now().plusSeconds(tempoValidadeSegundos),
            permissoes,
            null,
            null
        );
    }
    
    /**
     * Verifica se o token está expirado
     */
    public boolean isExpirado() {
        return dataExpiracao != null && LocalDateTime.now().isAfter(dataExpiracao);
    }
    
    /**
     * Retorna token mascarado para logs seguros
     */
    public String getTokenAcessoMascarado() {
        if (tokenAcesso == null || tokenAcesso.length() < 10) {
            return "[INVÁLIDO]";
        }
        return tokenAcesso.substring(0, 5) + "..." + tokenAcesso.substring(tokenAcesso.length() - 5);
    }
    
    /**
     * String segura para logs
     */
    public String toSecureString() {
        return String.format(
            "RespostaTokenDTO{tokenAcesso='%s', tipoToken='%s', expirado=%s, permissoes=%d}",
            getTokenAcessoMascarado(),
            tipoToken,
            isExpirado(),
            permissoes != null ? permissoes.size() : 0
        );
    }
    
    @Override
    public String toString() {
        return toSecureString();
    }
}