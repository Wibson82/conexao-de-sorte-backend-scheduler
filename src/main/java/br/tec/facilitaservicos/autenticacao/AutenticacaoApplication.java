package br.tec.facilitaservicos.autenticacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * ============================================================================
 * = APLICAÇÃO PRINCIPAL - MICROSERVIÇO AUTENTICAÇÃO
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
 * CARACTERÍSTICAS TÉCNICAS:
 * - Implementação 100% non-blocking
 * - Autenticação baseada em JWT com chaves RSA
 * - Integração com Azure Key Vault para gestão de chaves
 * - Rate limiting granular por endpoint
 * - Observabilidade completa (métricas, tracing, logs)
 * - Configuração externalizada para múltiplos ambientes
 * 
 * @author Sistema Conexão de Sorte
 * @version 1.0.0
 * @since 2024
 */
@SpringBootApplication
public class AutenticacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutenticacaoApplication.class, args);
    }
}