package br.tec.facilitaservicos.autenticacao.config;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;

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
        
        // Configurar valores das propriedades usando ReflectionTestUtils
        ReflectionTestUtils.setField(cacheConfig, "defaultTtl", Duration.ofMinutes(30));
        ReflectionTestUtils.setField(cacheConfig, "cacheNullValues", false);
    }

    @Test
    @DisplayName("Deve criar CacheManager com configurações corretas")
    void testCacheManagerCreation() {
        CacheManager cacheManager = cacheConfig.createRedisCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(org.springframework.data.redis.cache.RedisCacheManager.class);
    }

    @Test
    @DisplayName("Deve permitir obter cache para chaves RSA privadas")
    void testRsaPrivateKeyCache() {
        CacheManager cacheManager = cacheConfig.createRedisCacheManager(connectionFactory, meterRegistry);
        
        // Verifica se o cache pode ser obtido (será criado dinamicamente)
        assertThat(cacheManager.getCache("rsa-private-key")).isNotNull();
    }

    @Test
    @DisplayName("Deve permitir obter cache para chaves RSA públicas")
    void testRsaPublicKeyCache() {
        CacheManager cacheManager = cacheConfig.createRedisCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager.getCache("rsa-public-key")).isNotNull();
    }

    @Test
    @DisplayName("Deve permitir obter cache para Key IDs")
    void testKeyIdCache() {
        CacheManager cacheManager = cacheConfig.createRedisCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager.getCache("key-id")).isNotNull();
    }

    @Test
    @DisplayName("Deve permitir obter cache para tokens válidos")
    void testValidTokensCache() {
        CacheManager cacheManager = cacheConfig.createRedisCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager.getCache("valid-tokens")).isNotNull();
    }

    @Test
    @DisplayName("Deve permitir obter cache para JWKS")
    void testJwksCache() {
        CacheManager cacheManager = cacheConfig.createRedisCacheManager(connectionFactory, meterRegistry);
        
        assertThat(cacheManager.getCache("jwks")).isNotNull();
    }
}
