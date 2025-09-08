package br.tec.facilitaservicos.scheduler.infraestrutura.configuracao;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * 🔐 CONFIGURAÇÃO AZURE KEY VAULT - SCHEDULER SERVICE
 * ============================================================================
 * 
 * Configuração manual do Azure Key Vault para o microserviço Scheduler.
 * Carrega segredos automaticamente no contexto Spring usando OIDC.
 * 
 * 🎯 FUNCIONALIDADES:
 * - Autenticação OIDC com Azure (sem client-secret)
 * - Carregamento automático de segredos
 * - Fallback para variáveis de ambiente
 * - Logs estruturados de segurança
 * - Tratamento de erros resiliente
 * 
 * 🔒 SEGREDOS CARREGADOS:
 * - Banco de dados (R2DBC e JDBC)
 * - Redis (cache)
 * - JWT (validação)
 * - CORS e SSL
 * 
 * @author Sistema de Migração R2DBC - Scheduler
 * @version 1.0
 * @since 2024
 */
@Configuration
@ConditionalOnProperty(name = "AZURE_KEYVAULT_ENABLED", havingValue = "true")
public class AzureKeyVaultConfig {

    private static final Logger logger = LoggerFactory.getLogger(AzureKeyVaultConfig.class);

    @Value("${AZURE_KEYVAULT_ENDPOINT:}")
    private String keyVaultEndpoint;

    @Value("${AZURE_CLIENT_ID:}")
    private String clientId;

    @Value("${AZURE_TENANT_ID:}")
    private String tenantId;

    private final ConfigurableEnvironment environment;

    public AzureKeyVaultConfig(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    /**
     * Cliente do Azure Key Vault com autenticação OIDC
     */
    @Bean
    public SecretClient secretClient() {
        if (keyVaultEndpoint == null || keyVaultEndpoint.trim().isEmpty()) {
            logger.warn("🔒 Azure Key Vault endpoint não configurado. Usando fallback para variáveis de ambiente.");
            return null;
        }

        try {
            logger.info("🔐 Configurando Azure Key Vault client: {}", keyVaultEndpoint);
            
            return new SecretClientBuilder()
                .vaultUrl(keyVaultEndpoint)
                .credential(new DefaultAzureCredentialBuilder()
                    .managedIdentityClientId(clientId)
                    .build())
                .buildClient();
                
        } catch (Exception e) {
            logger.error("❌ Erro ao configurar Azure Key Vault client: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Carrega segredos do Key Vault no contexto Spring
     */
    @PostConstruct
    public void loadSecretsFromKeyVault() {
        SecretClient client = secretClient();
        if (client == null) {
            logger.warn("⚠️ Azure Key Vault client não disponível. Usando configurações locais.");
            return;
        }

        try {
            logger.info("🔄 Carregando segredos do Azure Key Vault...");
            
            Map<String, Object> secrets = new HashMap<>();
            
            // Segredos do banco de dados
            loadSecret(client, secrets, "conexao-de-sorte-database-r2dbc-url", "spring.r2dbc.url");
            loadSecret(client, secrets, "conexao-de-sorte-database-url", "spring.r2dbc.url"); // Fallback
            loadSecret(client, secrets, "conexao-de-sorte-database-username", "spring.r2dbc.username");
            loadSecret(client, secrets, "conexao-de-sorte-database-password", "spring.r2dbc.password");
            
            // Flyway (JDBC)
            loadSecret(client, secrets, "conexao-de-sorte-database-jdbc-url", "spring.flyway.url");
            loadSecret(client, secrets, "conexao-de-sorte-database-username", "spring.flyway.user");
            loadSecret(client, secrets, "conexao-de-sorte-database-password", "spring.flyway.password");
            
            // Redis
            loadSecret(client, secrets, "conexao-de-sorte-redis-host", "spring.data.redis.host");
            loadSecret(client, secrets, "conexao-de-sorte-redis-port", "spring.data.redis.port");
            loadSecret(client, secrets, "conexao-de-sorte-redis-password", "spring.data.redis.password");
            loadSecret(client, secrets, "conexao-de-sorte-redis-database", "spring.data.redis.database");
            
            // JWT
            loadSecret(client, secrets, "conexao-de-sorte-jwt-issuer", "spring.security.oauth2.resourceserver.jwt.issuer-uri");
            loadSecret(client, secrets, "conexao-de-sorte-jwt-jwks-uri", "spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
            
            // CORS
            loadSecret(client, secrets, "conexao-de-sorte-cors-allowed-origins", "cors.allowed-origins");
            loadSecret(client, secrets, "conexao-de-sorte-cors-allow-credentials", "cors.allow-credentials");
            
            // SSL
            loadSecret(client, secrets, "conexao-de-sorte-ssl-enabled", "server.ssl.enabled");
            loadSecret(client, secrets, "conexao-de-sorte-ssl-keystore-path", "server.ssl.key-store");
            loadSecret(client, secrets, "conexao-de-sorte-ssl-keystore-password", "server.ssl.key-store-password");
            
            // Adicionar segredos ao contexto Spring
            if (!secrets.isEmpty()) {
                MutablePropertySources propertySources = environment.getPropertySources();
                propertySources.addFirst(new MapPropertySource("azureKeyVaultSecrets", secrets));
                
                logger.info("✅ {} segredos carregados do Azure Key Vault", secrets.size());
            } else {
                logger.warn("⚠️ Nenhum segredo encontrado no Azure Key Vault");
            }
            
        } catch (Exception e) {
            logger.error("❌ Erro ao carregar segredos do Azure Key Vault: {}", e.getMessage());
            logger.warn("🔄 Continuando com configurações locais/variáveis de ambiente");
        }
    }

    /**
     * Carrega um segredo específico do Key Vault
     */
    private void loadSecret(SecretClient client, Map<String, Object> secrets, String secretName, String propertyName) {
        try {
            String secretValue = client.getSecret(secretName).getValue();
            if (secretValue != null && !secretValue.trim().isEmpty()) {
                secrets.put(propertyName, secretValue);
                logger.debug("🔐 Segredo carregado: {} -> {}", secretName, propertyName);
            }
        } catch (Exception e) {
            logger.debug("⚠️ Segredo não encontrado ou inacessível: {} ({})", secretName, e.getMessage());
        }
    }

    /**
     * Normaliza URL JDBC para R2DBC se necessário
     */
    private String normalizeR2dbcUrl(String url) {
        if (url != null && url.startsWith("jdbc:mysql://")) {
            return url.replace("jdbc:mysql://", "r2dbc:mysql://");
        }
        return url;
    }

    /**
     * Converte URL R2DBC para JDBC para Flyway
     */
    private String convertToJdbcUrl(String r2dbcUrl) {
        if (r2dbcUrl != null && r2dbcUrl.startsWith("r2dbc:mysql://")) {
            return r2dbcUrl.replace("r2dbc:mysql://", "jdbc:mysql://");
        }
        return r2dbcUrl;
    }
}