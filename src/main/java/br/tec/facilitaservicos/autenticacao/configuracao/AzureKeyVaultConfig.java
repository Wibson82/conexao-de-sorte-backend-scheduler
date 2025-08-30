package br.tec.facilitaservicos.autenticacao.configuracao;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ============================================================================
 * 🔐 CONFIGURAÇÃO AZURE KEY VAULT - SOLUÇÃO HÍBRIDA OIDC
 * ============================================================================
 * 
 * Implementação de acesso ao Azure Key Vault usando solução híbrida:
 * 
 * 1. CI/CD (GitHub Actions): Usa OIDC puro para obter secrets
 * 2. Runtime (Container): Usa Client Secret Credential com secrets injetados
 * 
 * VANTAGENS:
 * - ✅ OIDC puro no CI/CD (máxima segurança)
 * - ✅ Client Credentials no runtime (máxima compatibilidade) 
 * - ✅ Zero configuração no servidor (máxima simplicidade)
 * - ✅ Funciona em qualquer ambiente (não requer Azure VM)
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Configuration
@Profile("azure")
@ConfigurationProperties(prefix = "azure.keyvault")
@ConditionalOnProperty(name = "azure.keyvault.enabled", havingValue = "true", matchIfMissing = true)
public class AzureKeyVaultConfig {

    private static final Logger logger = LoggerFactory.getLogger(AzureKeyVaultConfig.class);

    private String endpoint;
    private String clientId;
    private String tenantId;
    private boolean enabled = true;
    private boolean fallbackEnabled = true;

    /**
     * Configura o cliente Azure Key Vault usando Client Secret Credential
     * 
     * As credenciais vêm de variáveis de ambiente injetadas pelo CI/CD
     * que obtém os secrets via OIDC do GitHub Actions
     */
    @Bean
    @ConditionalOnProperty(name = "azure.keyvault.enabled", havingValue = "true")
    public SecretClient secretClient() {
        logger.info("🔧 Configurando Azure Key Vault Client - Solução Híbrida OIDC");
        
        // Obter credenciais das variáveis de ambiente (injetadas pelo CI/CD)
        String runtimeClientId = getEnvironmentVariable("AZURE_CLIENT_ID", clientId);
        String runtimeTenantId = getEnvironmentVariable("AZURE_TENANT_ID", tenantId);
        String runtimeEndpoint = getEnvironmentVariable("AZURE_KEYVAULT_ENDPOINT", endpoint);
        
        if (isBlank(runtimeClientId) || isBlank(runtimeTenantId) || isBlank(runtimeEndpoint)) {
            
            logger.warn("⚠️ Azure Key Vault credentials incompletas. Verificar variáveis de ambiente:");
            logger.warn("   AZURE_CLIENT_ID: {}", isBlank(runtimeClientId) ? "❌ MISSING" : "✅ OK");
            logger.warn("   AZURE_TENANT_ID: {}", isBlank(runtimeTenantId) ? "❌ MISSING" : "✅ OK");
            logger.warn("   AZURE_KEYVAULT_ENDPOINT: {}", isBlank(runtimeEndpoint) ? "❌ MISSING" : "✅ OK");
            
            if (!fallbackEnabled) {
                throw new IllegalStateException("Azure Key Vault credentials incompletas e fallback desabilitado");
            }
            
            logger.warn("🔄 Fallback habilitado - aplicação continuará sem Azure Key Vault");
            return null;
        }

        try {
            // OIDC-only: usar DefaultAzureCredential (Workload Identity no runner)
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();

            // Criar cliente Key Vault
            SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl(runtimeEndpoint)
                    .credential(credential)
                    .buildClient();

            logger.info("✅ Azure Key Vault Client configurado com sucesso");
            logger.info("   Endpoint: {}", maskEndpoint(runtimeEndpoint));
            logger.info("   Client ID: {}", maskClientId(runtimeClientId));
            logger.info("   Tenant ID: {}", maskTenantId(runtimeTenantId));
            
            return secretClient;
            
        } catch (Exception e) {
            logger.error("❌ Erro ao configurar Azure Key Vault Client: {}", e.getMessage());
            
            if (!fallbackEnabled) {
                throw new RuntimeException("Falha ao configurar Azure Key Vault", e);
            }
            
            logger.warn("🔄 Fallback habilitado - aplicação continuará sem Azure Key Vault");
            return null;
        }
    }

    /**
     * Obter variável de ambiente com fallback para propriedade
     */
    private String getEnvironmentVariable(String envVar, String fallback) {
        String value = System.getenv(envVar);
        return isBlank(value) ? fallback : value;
    }

    /**
     * Verificar se string está vazia ou nula
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Mascarar endpoint para logs
     */
    private String maskEndpoint(String endpoint) {
        if (isBlank(endpoint)) return "N/A";
        return endpoint.replaceAll("([^/]+)\\.vault\\.azure\\.net", "*****.vault.azure.net");
    }

    /**
     * Mascarar Client ID para logs
     */
    private String maskClientId(String clientId) {
        if (isBlank(clientId) || clientId.length() < 8) return "N/A";
        return clientId.substring(0, 4) + "****" + clientId.substring(clientId.length() - 4);
    }

    /**
     * Mascarar Tenant ID para logs
     */
    private String maskTenantId(String tenantId) {
        if (isBlank(tenantId) || tenantId.length() < 8) return "N/A";
        return tenantId.substring(0, 4) + "****" + tenantId.substring(tenantId.length() - 4);
    }

    // Getters e Setters para ConfigurationProperties
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    // clientSecret removido (OIDC-only)

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
}
