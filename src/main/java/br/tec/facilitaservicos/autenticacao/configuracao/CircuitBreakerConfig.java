package br.tec.facilitaservicos.autenticacao.configuracao;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuração de Circuit Breakers para o Auth Service
 */
@Configuration
public class CircuitBreakerConfig {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreakerConfig.class);

    /**
     * Circuit Breaker para Database (R2DBC)
     */
    @Bean
    public CircuitBreaker databaseCircuitBreaker() {
        logger.info("🔧 Configurando Circuit Breaker para Database");
        
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(10)
            .minimumNumberOfCalls(3)
            .failureRateThreshold(50.0f)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .permittedNumberOfCallsInHalfOpenState(2)
            .slowCallRateThreshold(50.0f)
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("database", config);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                logger.warn("🔄 Database Circuit Breaker: {} -> {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState())
            )
            .onFailureRateExceeded(event -> 
                logger.error("❌ Database failure rate exceeded: {}%", 
                    event.getFailureRate())
            );
            
        return circuitBreaker;
    }

    /**
     * Circuit Breaker para Redis Cache
     */
    @Bean
    public CircuitBreaker redisCircuitBreaker() {
        logger.info("🔧 Configurando Circuit Breaker para Redis");
        
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(20)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(60.0f)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of("redis", config);
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                logger.info("🔄 Redis Circuit Breaker: {} -> {}", 
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState())
            );
            
        return circuitBreaker;
    }

    /**
     * Circuit Breaker para validação de tokens
     */
    @Bean
    public CircuitBreaker tokenValidationCircuitBreaker() {
        logger.info("🔧 Configurando Circuit Breaker para Token Validation");
        
        io.github.resilience4j.circuitbreaker.CircuitBreakerConfig config = 
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
            .slidingWindowSize(15)
            .minimumNumberOfCalls(5)
            .failureRateThreshold(40.0f)
            .waitDurationInOpenState(Duration.ofSeconds(20))
            .permittedNumberOfCallsInHalfOpenState(3)
            .build();

        return CircuitBreaker.of("token-validation", config);
    }

    /**
     * Retry para Database
     */
    @Bean
    public Retry databaseRetry() {
        logger.info("🔧 Configurando Retry para Database");
        
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .build();

        return Retry.of("database", config);
    }

    /**
     * Time Limiter para Database
     */
    @Bean
    public TimeLimiter databaseTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(5))
            .cancelRunningFuture(true)
            .build();
            
        return TimeLimiter.of("database", config);
    }

    /**
     * Time Limiter para Token Validation
     */
    @Bean
    public TimeLimiter tokenValidationTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(3))
            .cancelRunningFuture(true)
            .build();
            
        return TimeLimiter.of("token-validation", config);
    }
}