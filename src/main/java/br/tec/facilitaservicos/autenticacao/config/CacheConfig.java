package br.tec.facilitaservicos.autenticacao.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.metrics.cache.RedisCacheMetrics;
import io.micrometer.core.instrument.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuração crítica de cache para autenticação
 * TTL otimizado para segurança vs performance
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${spring.cache.redis.time-to-live:PT30M}")
    private Duration defaultTtl;

    @Value("${spring.cache.redis.cache-null-values:false}")
    private boolean cacheNullValues;

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public CacheManager authCacheManager(LettuceConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        return createRedisCacheManager(connectionFactory, meterRegistry);
    }

    /**
     * Método público para criar o CacheManager Redis - usado principalmente para testes
     */
    public CacheManager createRedisCacheManager(LettuceConnectionFactory connectionFactory, MeterRegistry meterRegistry) {
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
        
        if (!cacheNullValues) {
            defaultConfig = defaultConfig.disableCachingNullValues();
        }

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // CRÍTICO: Chaves RSA privadas (10 minutos - renovação automática)
        cacheConfigurations.put("rsa-private-key", defaultConfig
                .entryTtl(Duration.ofMinutes(10))
                .prefixCacheNameWith("auth:rsa:private:"));
        
        // CRÍTICO: Chaves RSA públicas (15 minutos - validação JWT)  
        cacheConfigurations.put("rsa-public-key", defaultConfig
                .entryTtl(Duration.ofMinutes(15))
                .prefixCacheNameWith("auth:rsa:public:"));
        
        // CRÍTICO: Key IDs (30 minutos - JWKS)
        cacheConfigurations.put("key-id", defaultConfig
                .entryTtl(Duration.ofMinutes(30))
                .prefixCacheNameWith("auth:keyid:"));
                
        // ALTO: Tokens válidos (5 minutos - validação rápida)
        cacheConfigurations.put("valid-tokens", defaultConfig
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("auth:token:"));
                
        // ALTO: JWKS cache (1 hora - endpoint público)
        cacheConfigurations.put("jwks", defaultConfig
                .entryTtl(Duration.ofHours(1))
                .prefixCacheNameWith("auth:jwks:"));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        // Métricas críticas para monitoramento de segurança
        // Registrar métricas para cada cache individualmente
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = (org.springframework.data.redis.cache.RedisCache) cacheManager.getCache(cacheName).getNativeCache();
            new RedisCacheMetrics(cache, java.util.Collections.singletonList(Tag.of("cache", cacheName)))
                    .bindTo(meterRegistry);
        });

        return cacheManager;
    }
}