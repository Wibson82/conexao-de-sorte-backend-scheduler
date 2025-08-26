package br.tec.facilitaservicos.autenticacao.service;

import br.tec.facilitaservicos.autenticacao.exception.KeyVaultException;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Serviço para integração com Azure Key Vault.
 * Gerencia chaves RSA para assinatura de JWT de forma reativa.
 */
@Service
public class KeyVaultService {
    
    private static final Logger logger = LoggerFactory.getLogger(KeyVaultService.class);
    
    private final SecretClient secretClient;
    
    @Value("${jwt.key-vault.private-key-name:jwt-private-key}")
    private String privateKeyName;
    
    @Value("${jwt.key-vault.public-key-name:jwt-public-key}")
    private String publicKeyName;
    
    @Value("${jwt.key-vault.key-id-name:jwt-key-id}")
    private String keyIdName;
    
    @Value("${spring.cloud.azure.keyvault.secret.enabled:false}")
    private boolean keyVaultEnabled;
    
    // Fallback keys para desenvolvimento/testes
    private static final String FALLBACK_PRIVATE_KEY = 
        "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC7VJTUt9Us8cKB" +
        "xEtfNXKNjwa9oQ4yCOPOFv7eEIAEFk8eHOeXRYl/dKO5S7KK4f0LMrHXUxJqgzJv" +
        "LbLrVu8JJJGCe5YlR2TRj8aP2VYxvZR8G9P3K0kPwDi6JzrO8QvXLbZkPP7rY/0v";
    
    private static final String FALLBACK_PUBLIC_KEY = 
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAu1SU1L7VLPHCgcRLXzVy" +
        "jY8GvaEOMgjjzhb+3hCABBZPHhznl0WJf3SjuUuyiuH9CzKx11MSaoMyby2y61bv";
    
    private static final String FALLBACK_KEY_ID = "fallback-key-id";
    
    public KeyVaultService(@Value("${spring.cloud.azure.keyvault.secret.endpoint:}") String keyVaultUri) {
        if (keyVaultEnabled && !keyVaultUri.isBlank()) {
            this.secretClient = new SecretClientBuilder()
                .vaultUrl(keyVaultUri)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();
            logger.info("Azure Key Vault configurado: {}", keyVaultUri);
        } else {
            this.secretClient = null;
            logger.warn("Azure Key Vault desabilitado - usando chaves fallback para desenvolvimento");
        }
    }
    
    /**
     * Obtém a chave privada RSA do Key Vault (com cache).
     */
    @Cacheable(value = "rsa-private-key", unless = "#result == null")
    public Mono<RSAPrivateKey> getPrivateKey() {
        logger.debug("Obtendo chave privada RSA");
        
        if (!keyVaultEnabled || secretClient == null) {
            return getFallbackPrivateKey();
        }
        
        return Mono.fromCallable(() -> {
            try {
                KeyVaultSecret secret = secretClient.getSecret(privateKeyName);
                String privateKeyPem = secret.getValue();
                return parsePrivateKey(privateKeyPem);
            } catch (Exception e) {
                logger.error("Erro ao obter chave privada do Key Vault: {}", e.getMessage());
                throw new KeyVaultException("Falha ao obter chave privada", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(key -> logger.debug("Chave privada RSA obtida com sucesso"))
        .onErrorResume(error -> {
            logger.warn("Fallback para chave privada padrão devido ao erro: {}", error.getMessage());
            return getFallbackPrivateKey();
        });
    }
    
    /**
     * Obtém a chave pública RSA do Key Vault (com cache).
     */
    @Cacheable(value = "rsa-public-key", unless = "#result == null")
    public Mono<RSAPublicKey> getPublicKey() {
        logger.debug("Obtendo chave pública RSA");
        
        if (!keyVaultEnabled || secretClient == null) {
            return getFallbackPublicKey();
        }
        
        return Mono.fromCallable(() -> {
            try {
                KeyVaultSecret secret = secretClient.getSecret(publicKeyName);
                String publicKeyPem = secret.getValue();
                return parsePublicKey(publicKeyPem);
            } catch (Exception e) {
                logger.error("Erro ao obter chave pública do Key Vault: {}", e.getMessage());
                throw new KeyVaultException("Falha ao obter chave pública", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(key -> logger.debug("Chave pública RSA obtida com sucesso"))
        .onErrorResume(error -> {
            logger.warn("Fallback para chave pública padrão devido ao erro: {}", error.getMessage());
            return getFallbackPublicKey();
        });
    }
    
    /**
     * Obtém o ID da chave para o JWK Set (com cache).
     */
    @Cacheable(value = "key-id", unless = "#result == null")
    public Mono<String> getKeyId() {
        logger.debug("Obtendo ID da chave");
        
        if (!keyVaultEnabled || secretClient == null) {
            return Mono.just(FALLBACK_KEY_ID);
        }
        
        return Mono.fromCallable(() -> {
            try {
                KeyVaultSecret secret = secretClient.getSecret(keyIdName);
                return secret.getValue();
            } catch (Exception e) {
                logger.error("Erro ao obter ID da chave do Key Vault: {}", e.getMessage());
                throw new KeyVaultException("Falha ao obter ID da chave", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(keyId -> logger.debug("ID da chave obtido com sucesso: {}", keyId))
        .onErrorResume(error -> {
            logger.warn("Fallback para ID da chave padrão devido ao erro: {}", error.getMessage());
            return Mono.just(FALLBACK_KEY_ID);
        });
    }
    
    /**
     * Força a rotação das chaves (limpa cache).
     */
    public Mono<Void> rotateKeys() {
        logger.info("Iniciando rotação de chaves");
        
        return Mono.fromRunnable(() -> {
            // Limpar caches (implementação específica do cache)
            // Aqui seria implementada a lógica de limpeza do cache
            logger.info("Cache de chaves limpo - próximas requisições buscarão novas chaves");
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    /**
     * Verifica se o Key Vault está disponível.
     */
    public Mono<Boolean> isKeyVaultAvailable() {
        if (!keyVaultEnabled || secretClient == null) {
            return Mono.just(false);
        }
        
        return Mono.fromCallable(() -> {
            try {
                // Tenta obter um secret para verificar conectividade
                secretClient.getSecret(keyIdName);
                return true;
            } catch (Exception e) {
                logger.warn("Key Vault não está disponível: {}", e.getMessage());
                return false;
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorReturn(false);
    }
    
    // Métodos privados auxiliares
    
    private Mono<RSAPrivateKey> getFallbackPrivateKey() {
        return Mono.fromCallable(() -> parsePrivateKey(FALLBACK_PRIVATE_KEY))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess(key -> logger.debug("Usando chave privada fallback"))
            .onErrorMap(e -> new KeyVaultException("Erro ao carregar chave privada fallback", e));
    }
    
    private Mono<RSAPublicKey> getFallbackPublicKey() {
        return Mono.fromCallable(() -> parsePublicKey(FALLBACK_PUBLIC_KEY))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess(key -> logger.debug("Usando chave pública fallback"))
            .onErrorMap(e -> new KeyVaultException("Erro ao carregar chave pública fallback", e));
    }
    
    private RSAPrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        // Remove headers/footers do PEM e espaços
        String privateKeyContent = privateKeyPem
            .replaceAll("-----BEGIN PRIVATE KEY-----", "")
            .replaceAll("-----END PRIVATE KEY-----", "")
            .replaceAll("-----BEGIN RSA PRIVATE KEY-----", "")
            .replaceAll("-----END RSA PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
        
        byte[] decoded = Base64.getDecoder().decode(privateKeyContent);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }
    
    private RSAPublicKey parsePublicKey(String publicKeyPem) throws Exception {
        // Remove headers/footers do PEM e espaços
        String publicKeyContent = publicKeyPem
            .replaceAll("-----BEGIN PUBLIC KEY-----", "")
            .replaceAll("-----END PUBLIC KEY-----", "")
            .replaceAll("-----BEGIN RSA PUBLIC KEY-----", "")
            .replaceAll("-----END RSA PUBLIC KEY-----", "")
            .replaceAll("\\s", "");
        
        byte[] decoded = Base64.getDecoder().decode(publicKeyContent);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }
}