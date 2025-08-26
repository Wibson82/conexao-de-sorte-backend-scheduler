package br.tec.facilitaservicos.autenticacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * ============================================================================
 * = APLICAÇÃO PRINCIPAL - MICROSERVIÇO AUTENTICAÇÃO
 * ============================================================================
 * 
 * Microserviço de autenticação 100% reativo usando:
 * - Spring Boot 3.5+
 * - WebFlux (reativo)
 * - R2DBC (reativo)
 * - Spring Security reativo
 * - JWT com JWKS
 * - Azure Key Vault para rotação de chaves
 * - Observabilidade com Micrometer
 * - Resilience4j para rate limiting
 * 
 * Endpoints:
 * - POST /auth/token - Geração de token
 * - POST /auth/refresh - Renovação de token
 * - POST /auth/introspect - Introspecção de token
 * - GET /oauth2/jwks - JWK Set público
 * 
 * ============================================================================
 */
@SpringBootApplication
@EnableR2dbcRepositories
@EnableR2dbcAuditing
public class AutenticacaoApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(AutenticacaoApplication.class, args);
    }
}