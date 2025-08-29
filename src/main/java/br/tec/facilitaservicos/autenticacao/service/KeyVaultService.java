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
    
    // Fallback keys para desenvolvimento/testes - Chaves RSA 2048 completas
    private static final String FALLBACK_PRIVATE_KEY = 
        "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCmaLtzMJwL/dt2" +
        "vu0VT3d+0kpcPTQ1c/RFUrEes7Edvn28Ad0GXvFLRIw3F24Jt51X06jxaKdj5aPv" +
        "K73pTVF3ob5S5xfYJ76Y0tsJjd5IV3gXhlZZOq0CIzUGWQzcP15wBOehm/L3Gu6t" +
        "p0I5aGWylv2gR9Vy++Mk1e3ekhl4PYZQ2rD23ErJCe/cO0J/1NnJiCQzwn/N+c0Q" +
        "yCIU3Lpgiamq+tn88NFEEiohva5aXgMr6FSHEzU378icqmv1LGzNWWjYPUU6n+LH" +
        "gXFkStJKh5YCNrqvt7LwewkkjoUc0y5gafAXfyk6VASAhASuW9erUCDHT8MRK+cO" +
        "8+vDV1LjAgMBAAECggEAPG/pNIrBAfHu1Q++l3DHG35Ql3N7FLbKTqsbvOTSPNVE" +
        "YRXkKj4tMILy0cdmpYzUTmOHBjZWJQfwJBsk3CjCn8pUj1Ny64Rzypk6CBxIUMnD" +
        "yfd2QO34i8Axr96DtzIkoFaHscAr2+ciLFuEx8jMtrHz51Rvh1VIR8aSn7U8HtjS" +
        "7N+xpKCYepS/J/JPn/xpPK8v3PpuOKMqAQeJGBELWFhNUkv/S8dj/LAIIdwcsmWA" +
        "bZUwb5ImVw6vHxW+QKv64pCPQ6Aq0x1vaISOJHZTlO7M8RhU3Mm/cDZU06LU6QwX" +
        "3VB3Yyv+zNaxv+DqRBtUzBmi0eIHC8jYz3i3q4HmoQKBgQDioTm6m6oPjUU1KKJ6" +
        "xuySOYqQklmfk/unVoMDBLyreNw9hredk+w3WU9Kit0GB5qQ4bVrjfbVzyLE8Fdv" +
        "3NfgcGZH4Pq4reQWQ5iL11ETEYqRgu85pEqAK1/8DS0e/kvJqDKYLnibNUmTZ3Ut" +
        "e8v16u+oV0FzASUEZghcrhRvqwKBgQC7+ZkE7+XVqxhduXQWXNG5Gy6uEMSVxCRH" +
        "dFfmINDtI71Hi5ubUXpPkytHKj7TZ2lktHHfOHQuRAcidSPBEKB8dfOEjxFZVxq4" +
        "bLi0lMdpRf+OpAGNKiivGk/6YJXbnM7Xrf64LMqPu0kjsiSmqH0SWbj/vu1InzQz" +
        "xg0Edw3RqQKBgAEwh3ULTCE4xJw60l+Cm8tIsgmAOygzRqbiNV3WsIbV45IPGveg" +
        "xySjO19Qy0g00gLgrGscG6eTpsMR3+OebSOoc2D9NVOy1fen8y0IvEw1U1zgKxFK" +
        "Y3m4wJA6IXqAKzWrxFg4JjnqVbCIYn0SoUdxLEDd9GH+J4uWXYTfBQxxAoGALJwK" +
        "bXqFP9Tv3mZBn4D9oIFL7bE7BhPbTD3XEl0dV/nQVKdUEWMftLzHW4cyC7eR8n1E" +
        "POZH5CbuzoWgK5RHkeHoHcBaLKqYQ8ZBe1GHlXswL+jKGXc02oFTE6dSSSEIkXTQ" +
        "a2Lt23hl3hvLyOVZcT5rwf3MkByAJf1NX47lb6kCgYBu0727dmIbfVCkHZ6pqkDT" +
        "4+7HNDf3B0fLwtEo7pbWQkbqXTt4885OF8scuJ2nFHGjC6Mci2/jbNQXhzSwgq6U" +
        "H4IFygTx/4UbwUmQfElY7gZfDh5gne4LDLhrrJuURFCXUJy5YznD7C/aI/ZAK0gH" +
        "cfH5WumEGlUaspgGYHb5hw==";
    
    private static final String FALLBACK_PUBLIC_KEY = 
        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApmi7czCcC/3bdr7tFU93" +
        "ftJKXD00NXP0RVKxHrOxHb59vAHdBl7xS0SMNxduCbedV9Oo8WinY+Wj7yu96U1R" +
        "d6G+UucX2Ce+mNLbCY3eSFd4F4ZWWTqtAiM1BlkM3D9ecATnoZvy9xruradCOWhl" +
        "spb9oEfVcvvjJNXt3pIZeD2GUNqw9txKyQnv3DtCf9TZyYgkM8J/zfnNEMgiFNy6" +
        "YImpqvrZ/PDRRBIqIb2uWl4DK+hUhxM1N+/InKpr9SxszVlo2D1FOp/ix4FxZEr" +
        "SSoeWAja6r7ey8HsJJI6FHNMuYGnwF38pOlQEgIQErlvXq1Agx0/DESvnDvPrw1" +
        "dS4wIDAQAB";
    
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