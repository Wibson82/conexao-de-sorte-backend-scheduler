package br.tec.facilitaservicos.autenticacao.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuração de Rate Limiting usando Resilience4j.
 * Implementa controle de taxa de requisições por endpoint.
 */
@Configuration
public class RateLimitConfig {
    
    /**
     * Rate limiter para endpoint de login.
     * Mais restritivo devido à natureza sensível.
     */
    @Bean("authTokenRateLimiter")
    public RateLimiter authTokenRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(5) // 5 tentativas
            .limitRefreshPeriod(Duration.ofMinutes(1)) // por minuto
            .timeoutDuration(Duration.ofSeconds(3)) // timeout de 3s
            .build();
            
        return RateLimiter.of("auth-token", config);
    }
    
    /**
     * Rate limiter para endpoint de refresh token.
     * Menos restritivo que login.
     */
    @Bean("authRefreshRateLimiter") 
    public RateLimiter authRefreshRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(15) // 15 tentativas
            .limitRefreshPeriod(Duration.ofMinutes(1)) // por minuto
            .timeoutDuration(Duration.ofSeconds(3))
            .build();
            
        return RateLimiter.of("auth-refresh", config);
    }
    
    /**
     * Rate limiter para endpoint de introspecção.
     * Mais permissivo para validação de tokens.
     */
    @Bean("authIntrospectRateLimiter")
    public RateLimiter authIntrospectRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(100) // 100 verificações
            .limitRefreshPeriod(Duration.ofMinutes(1)) // por minuto
            .timeoutDuration(Duration.ofSeconds(1))
            .build();
            
        return RateLimiter.of("auth-introspect", config);
    }
    
    /**
     * Rate limiter geral para outros endpoints.
     */
    @Bean("generalRateLimiter")
    public RateLimiter generalRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
            .limitForPeriod(60) // 60 requisições
            .limitRefreshPeriod(Duration.ofMinutes(1)) // por minuto
            .timeoutDuration(Duration.ofSeconds(2))
            .build();
            
        return RateLimiter.of("general", config);
    }
}