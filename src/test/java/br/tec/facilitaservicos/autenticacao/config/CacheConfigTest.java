package br.tec.facilitaservicos.autenticacao.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.context.TestPropertySource;

import io.micrometer.core.instrument.MeterRegistry;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.cache.type=redis",
    "spring.cache.redis.time-to-live=PT30M",
    "spring.cache.redis.cache-null-values=false"
})
@DisplayName("Testes da CacheConfig")
class CacheConfigTest {

    private CacheConfig cacheConfig;
    private LettuceConnectionFactory connectionFactory;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        cacheConfig = new CacheConfig();
        connectionFactory = mock(LettuceConnectionFactory.class);
        meterRegistry = mock(MeterRegistry.class);
    }

    @Test
    @DisplayName("Deve criar CacheManager com configurações corretas")
    void testCacheManagerCreation() {
        CacheManager cacheManager = cacheConfig.authCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager.getCacheNames()).isNotEmpty();
    }

    @Test
    @DisplayName("Deve configurar cache para chaves RSA privadas")
    void testRsaPrivateKeyCache() {
        CacheManager cacheManager = cacheConfig.authCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager.getCacheNames()).contains("rsa-private-key");
    }

    @Test
    @DisplayName("Deve configurar cache para chaves RSA públicas")
    void testRsaPublicKeyCache() {
        CacheManager cacheManager = cacheConfig.authCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager.getCacheNames()).contains("rsa-public-key");
    }

    @Test
    @DisplayName("Deve configurar cache para Key IDs")
    void testKeyIdCache() {
        CacheManager cacheManager = cacheConfig.authCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager.getCacheNames()).contains("key-id");
    }

    @Test
    @DisplayName("Deve configurar cache para tokens válidos")
    void testValidTokensCache() {
        CacheManager cacheManager = cacheConfig.authCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager.getCacheNames()).contains("valid-tokens");
    }

    @Test
    @DisplayName("Deve configurar cache para JWKS")
    void testJwksCache() {
        CacheManager cacheManager = cacheConfig.authCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager.getCacheNames()).contains("jwks");
    }
}
