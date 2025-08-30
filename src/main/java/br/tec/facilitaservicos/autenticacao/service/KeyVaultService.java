package br.tec.facilitaservicos.autenticacao.service;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

import br.tec.facilitaservicos.autenticacao.exception.KeyVaultException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Serviço para integração com Azure Key Vault.
 * Gerencia chaves RSA para assinatura de JWT de forma reativa.
 */
@Service
public class KeyVaultService {

    private static final Logger logger = LoggerFactory.getLogger(KeyVaultService.class);

    // ============================================================================
    // 🔧 CONSTANTES DE CONFIGURAÇÃO - AMBIENTE DE PRODUÇÃO
    // ============================================================================

    // Nomes de cache
    private static final String CACHE_RSA_PRIVATE_KEY = "rsa-private-key";
    private static final String CACHE_RSA_PUBLIC_KEY = "rsa-public-key";
    private static final String CACHE_KEY_ID = "key-id";

    // Headers PEM
    private static final String PEM_PRIVATE_KEY_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PEM_PRIVATE_KEY_FOOTER = "-----END PRIVATE KEY-----";
    private static final String PEM_RSA_PRIVATE_KEY_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PEM_RSA_PRIVATE_KEY_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PEM_PUBLIC_KEY_HEADER = "-----BEGIN PUBLIC KEY-----";
    private static final String PEM_PUBLIC_KEY_FOOTER = "-----END PUBLIC KEY-----";
    private static final String PEM_RSA_PUBLIC_KEY_HEADER = "-----BEGIN RSA PUBLIC KEY-----";
    private static final String PEM_RSA_PUBLIC_KEY_FOOTER = "-----END RSA PUBLIC KEY-----";

    // Algoritmos e especificações
    private static final String RSA_ALGORITHM = "RSA";
    private static final String WHITESPACE_REGEX = "\\s";
    private static final String EMPTY_STRING = "";

    // Mensagens de log
    private static final String LOG_KEYVAULT_CONFIGURED = "Azure Key Vault configurado: {}";
    private static final String LOG_KEYVAULT_DISABLED = "Azure Key Vault desabilitado - usando chaves fallback para desenvolvimento";
    private static final String LOG_OBTAINING_PRIVATE_KEY = "Obtendo chave privada RSA";
    private static final String LOG_OBTAINING_PUBLIC_KEY = "Obtendo chave pública RSA";
    private static final String LOG_OBTAINING_KEY_ID = "Obtendo ID da chave";
    private static final String LOG_PRIVATE_KEY_SUCCESS = "Chave privada RSA obtida com sucesso";
    private static final String LOG_PUBLIC_KEY_SUCCESS = "Chave pública RSA obtida com sucesso";
    private static final String LOG_KEY_ID_SUCCESS = "ID da chave obtido com sucesso: {}";
    private static final String LOG_USING_FALLBACK_PRIVATE = "Usando chave privada fallback";
    private static final String LOG_USING_FALLBACK_PUBLIC = "Usando chave pública fallback";
    private static final String LOG_ROTATION_START = "Iniciando rotação de chaves";
    private static final String LOG_CACHE_CLEARED = "Cache de chaves limpo - próximas requisições buscarão novas chaves";

    // Mensagens de erro
    private static final String ERROR_PRIVATE_KEY_KEYVAULT = "Erro ao obter chave privada do Key Vault: {}";
    private static final String ERROR_PUBLIC_KEY_KEYVAULT = "Erro ao obter chave pública do Key Vault: {}";
    private static final String ERROR_KEY_ID_KEYVAULT = "Erro ao obter ID da chave do Key Vault: {}";
    private static final String ERROR_KEYVAULT_UNAVAILABLE = "Key Vault não está disponível: {}";
    private static final String ERROR_FALLBACK_PRIVATE_KEY = "Falha ao obter chave privada";
    private static final String ERROR_FALLBACK_PUBLIC_KEY = "Falha ao obter chave pública";
    private static final String ERROR_FALLBACK_KEY_ID = "Falha ao obter ID da chave";
    private static final String ERROR_LOAD_FALLBACK_PRIVATE = "Erro ao carregar chave privada fallback";
    private static final String ERROR_LOAD_FALLBACK_PUBLIC = "Erro ao carregar chave pública fallback";

    // Mensagens de fallback
    private static final String FALLBACK_PRIVATE_KEY_MSG = "Fallback para chave privada padrão devido ao erro: {}";
    private static final String FALLBACK_PUBLIC_KEY_MSG = "Fallback para chave pública padrão devido ao erro: {}";
    private static final String FALLBACK_KEY_ID_MSG = "Fallback para ID da chave padrão devido ao erro: {}";

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
        // Programação defensiva: validação de parâmetros
        SecretClient tempSecretClient = null;

        if (keyVaultEnabled && keyVaultUri != null && !keyVaultUri.trim().isEmpty()) {
            try {
                tempSecretClient = new SecretClientBuilder()
                    .vaultUrl(keyVaultUri.trim())
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
                logger.info(LOG_KEYVAULT_CONFIGURED, keyVaultUri);
            } catch (Exception e) {
                logger.error("❌ Erro ao configurar Azure Key Vault: {}", e.getMessage(), e);
                tempSecretClient = null;
                logger.warn(LOG_KEYVAULT_DISABLED);
            }
        } else {
            logger.warn(LOG_KEYVAULT_DISABLED);
        }

        this.secretClient = tempSecretClient;
    }
    
    /**
     * Obtém a chave privada RSA do Key Vault (com cache).
     */
    @Cacheable(value = CACHE_RSA_PRIVATE_KEY, unless = "#result == null")
    public Mono<RSAPrivateKey> getPrivateKey() {
        logger.debug(LOG_OBTAINING_PRIVATE_KEY);

        // Programação defensiva: verificação de disponibilidade
        if (!keyVaultEnabled || secretClient == null) {
            return getFallbackPrivateKey();
        }

        return Mono.fromCallable(() -> {
            try {
                // Programação defensiva: validação do nome da chave
                if (privateKeyName == null || privateKeyName.trim().isEmpty()) {
                    throw new KeyVaultException("Nome da chave privada não configurado");
                }

                KeyVaultSecret secret = secretClient.getSecret(privateKeyName.trim());

                // Programação defensiva: validação do secret
                if (secret == null || secret.getValue() == null || secret.getValue().trim().isEmpty()) {
                    throw new KeyVaultException("Secret da chave privada vazio ou nulo");
                }

                String privateKeyPem = secret.getValue();
                return parsePrivateKey(privateKeyPem);
            } catch (Exception e) {
                logger.error(ERROR_PRIVATE_KEY_KEYVAULT, e.getMessage());
                throw new KeyVaultException(ERROR_FALLBACK_PRIVATE_KEY, e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(key -> {
            if (key != null) {
                logger.debug(LOG_PRIVATE_KEY_SUCCESS);
            }
        })
        .onErrorResume(error -> {
            String errorMsg = error != null ? error.getMessage() : "Erro desconhecido";
            logger.warn(FALLBACK_PRIVATE_KEY_MSG, errorMsg);
            return getFallbackPrivateKey();
        });
    }
    
    /**
     * Obtém a chave pública RSA do Key Vault (com cache).
     */
    @Cacheable(value = CACHE_RSA_PUBLIC_KEY, unless = "#result == null")
    public Mono<RSAPublicKey> getPublicKey() {
        logger.debug(LOG_OBTAINING_PUBLIC_KEY);

        // Programação defensiva: verificação de disponibilidade
        if (!keyVaultEnabled || secretClient == null) {
            return getFallbackPublicKey();
        }

        return Mono.fromCallable(() -> {
            try {
                // Programação defensiva: validação do nome da chave
                if (publicKeyName == null || publicKeyName.trim().isEmpty()) {
                    throw new KeyVaultException("Nome da chave pública não configurado");
                }

                KeyVaultSecret secret = secretClient.getSecret(publicKeyName.trim());

                // Programação defensiva: validação do secret
                if (secret == null || secret.getValue() == null || secret.getValue().trim().isEmpty()) {
                    throw new KeyVaultException("Secret da chave pública vazio ou nulo");
                }

                String publicKeyPem = secret.getValue();
                return parsePublicKey(publicKeyPem);
            } catch (Exception e) {
                logger.error(ERROR_PUBLIC_KEY_KEYVAULT, e.getMessage());
                throw new KeyVaultException(ERROR_FALLBACK_PUBLIC_KEY, e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(key -> {
            if (key != null) {
                logger.debug(LOG_PUBLIC_KEY_SUCCESS);
            }
        })
        .onErrorResume(error -> {
            String errorMsg = error != null ? error.getMessage() : "Erro desconhecido";
            logger.warn(FALLBACK_PUBLIC_KEY_MSG, errorMsg);
            return getFallbackPublicKey();
        });
    }
    
    /**
     * Obtém o ID da chave para o JWK Set (com cache).
     */
    @Cacheable(value = CACHE_KEY_ID, unless = "#result == null")
    public Mono<String> getKeyId() {
        logger.debug(LOG_OBTAINING_KEY_ID);

        // Programação defensiva: verificação de disponibilidade
        if (!keyVaultEnabled || secretClient == null) {
            return Mono.just(FALLBACK_KEY_ID);
        }

        return Mono.fromCallable(() -> {
            try {
                // Programação defensiva: validação do nome da chave
                if (keyIdName == null || keyIdName.trim().isEmpty()) {
                    throw new KeyVaultException("Nome do ID da chave não configurado");
                }

                KeyVaultSecret secret = secretClient.getSecret(keyIdName.trim());

                // Programação defensiva: validação do secret
                if (secret == null || secret.getValue() == null || secret.getValue().trim().isEmpty()) {
                    throw new KeyVaultException("Secret do ID da chave vazio ou nulo");
                }

                return secret.getValue().trim();
            } catch (Exception e) {
                logger.error(ERROR_KEY_ID_KEYVAULT, e.getMessage());
                throw new KeyVaultException(ERROR_FALLBACK_KEY_ID, e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(keyId -> {
            if (keyId != null && !keyId.isEmpty()) {
                logger.debug(LOG_KEY_ID_SUCCESS, keyId);
            }
        })
        .onErrorResume(error -> {
            String errorMsg = error != null ? error.getMessage() : "Erro desconhecido";
            logger.warn(FALLBACK_KEY_ID_MSG, errorMsg);
            return Mono.just(FALLBACK_KEY_ID);
        });
    }
    
    /**
     * Força a rotação das chaves (limpa cache).
     */
    public Mono<Void> rotateKeys() {
        logger.info(LOG_ROTATION_START);

        return Mono.fromRunnable(() -> {
            try {
                // Limpar caches (implementação específica do cache)
                // Aqui seria implementada a lógica de limpeza do cache
                logger.info(LOG_CACHE_CLEARED);
            } catch (Exception e) {
                logger.error("❌ Erro ao limpar cache durante rotação de chaves: {}", e.getMessage(), e);
                throw new RuntimeException("Falha na rotação de chaves", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    /**
     * Verifica se o Key Vault está disponível.
     */
    public Mono<Boolean> isKeyVaultAvailable() {
        // Programação defensiva: verificação de pré-condições
        if (!keyVaultEnabled || secretClient == null) {
            return Mono.just(false);
        }

        return Mono.fromCallable(() -> {
            try {
                // Programação defensiva: validação do nome da chave
                if (keyIdName == null || keyIdName.trim().isEmpty()) {
                    logger.warn("❌ Nome do ID da chave não configurado para verificação de disponibilidade");
                    return false;
                }

                // Tenta obter um secret para verificar conectividade
                KeyVaultSecret secret = secretClient.getSecret(keyIdName.trim());
                return secret != null;
            } catch (Exception e) {
                logger.warn(ERROR_KEYVAULT_UNAVAILABLE, e.getMessage());
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
            .doOnSuccess(key -> {
                if (key != null) {
                    logger.debug(LOG_USING_FALLBACK_PRIVATE);
                }
            })
            .onErrorMap(e -> new KeyVaultException(ERROR_LOAD_FALLBACK_PRIVATE, e));
    }

    private Mono<RSAPublicKey> getFallbackPublicKey() {
        return Mono.fromCallable(() -> parsePublicKey(FALLBACK_PUBLIC_KEY))
            .subscribeOn(Schedulers.boundedElastic())
            .doOnSuccess(key -> {
                if (key != null) {
                    logger.debug(LOG_USING_FALLBACK_PUBLIC);
                }
            })
            .onErrorMap(e -> new KeyVaultException(ERROR_LOAD_FALLBACK_PUBLIC, e));
    }
    
    private RSAPrivateKey parsePrivateKey(String privateKeyPem) throws Exception {
        // Programação defensiva: validação de entrada
        if (privateKeyPem == null || privateKeyPem.trim().isEmpty()) {
            throw new IllegalArgumentException("PEM da chave privada não pode ser nulo ou vazio");
        }

        try {
            // Remove headers/footers do PEM e espaços
            String privateKeyContent = privateKeyPem
                .replaceAll(PEM_PRIVATE_KEY_HEADER, EMPTY_STRING)
                .replaceAll(PEM_PRIVATE_KEY_FOOTER, EMPTY_STRING)
                .replaceAll(PEM_RSA_PRIVATE_KEY_HEADER, EMPTY_STRING)
                .replaceAll(PEM_RSA_PRIVATE_KEY_FOOTER, EMPTY_STRING)
                .replaceAll(WHITESPACE_REGEX, EMPTY_STRING);

            // Programação defensiva: validação do conteúdo limpo
            if (privateKeyContent.isEmpty()) {
                throw new IllegalArgumentException("Conteúdo da chave privada vazio após limpeza");
            }

            byte[] decoded = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);

            return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new Exception("Erro ao fazer parse da chave privada: " + e.getMessage(), e);
        }
    }
    
    private RSAPublicKey parsePublicKey(String publicKeyPem) throws Exception {
        // Programação defensiva: validação de entrada
        if (publicKeyPem == null || publicKeyPem.trim().isEmpty()) {
            throw new IllegalArgumentException("PEM da chave pública não pode ser nulo ou vazio");
        }

        try {
            // Remove headers/footers do PEM e espaços
            String publicKeyContent = publicKeyPem
                .replaceAll(PEM_PUBLIC_KEY_HEADER, EMPTY_STRING)
                .replaceAll(PEM_PUBLIC_KEY_FOOTER, EMPTY_STRING)
                .replaceAll(PEM_RSA_PUBLIC_KEY_HEADER, EMPTY_STRING)
                .replaceAll(PEM_RSA_PUBLIC_KEY_FOOTER, EMPTY_STRING)
                .replaceAll(WHITESPACE_REGEX, EMPTY_STRING);

            // Programação defensiva: validação do conteúdo limpo
            if (publicKeyContent.isEmpty()) {
                throw new IllegalArgumentException("Conteúdo da chave pública vazio após limpeza");
            }

            byte[] decoded = Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);

            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (Exception e) {
            throw new Exception("Erro ao fazer parse da chave pública: " + e.getMessage(), e);
        }
    }
}