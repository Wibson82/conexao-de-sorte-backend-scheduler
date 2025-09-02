package br.tec.facilitaservicos.autenticacao.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração de segurança reativa para WebFlux.
 * Define políticas de segurança e CORS para o microserviço.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    /**
     * Configuração da cadeia de filtros de segurança.
     */
    @Bean
    public SecurityWebFilterChain securityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authorizeExchange(exchanges -> exchanges
                // Endpoints públicos - incluindo health checks para load balancer
                .pathMatchers(
                    "/auth/**",
                    "/.well-known/**", 
                    "/oauth2/jwks**",
                    "/actuator/health**",
                    "/actuator/health/liveness**",
                    "/actuator/health/readiness**",
                    "/actuator/info",
                    "/actuator/prometheus",
                    "/v3/api-docs**",
                    "/swagger-ui**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // Endpoints do Actuator sensíveis (requer autenticação)
                .pathMatchers("/actuator/metrics/**", "/actuator/env**", "/actuator/configprops**").authenticated()
                // Outros endpoints do actuator são públicos para monitoramento
                .pathMatchers("/actuator/**").permitAll()
                // Outros endpoints requerem autenticação
                .anyExchange().authenticated()
            )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            .build();
    }
    
    /**
     * Configuração de CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origens permitidas baseadas em variáveis de ambiente
        String allowedOrigins = System.getenv("conexao-de-sorte-cors-allowed-origins");
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            configuration.setAllowedOriginPatterns(List.of(allowedOrigins.split(",")));
        } else {
            // Fallback para desenvolvimento
            configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://*.conexaodesorte.com",
                "https://*.facilitaservicos.com.br"
            ));
        }
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"
        ));
        
        // Headers permitidos
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "X-Request-ID",
            "X-Trace-ID"
        ));
        
        // Headers expostos
        configuration.setExposedHeaders(List.of(
            "X-Request-ID",
            "X-Trace-ID",
            "X-Rate-Limit-Remaining",
            "X-Rate-Limit-Reset"
        ));
        
        // Allow credentials baseado em variável de ambiente
        String allowCredentials = System.getenv("conexao-de-sorte-cors-allow-credentials");
        configuration.setAllowCredentials(Boolean.parseBoolean(allowCredentials != null ? allowCredentials : "true"));
        configuration.setMaxAge(3600L); // 1 hora
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
    
    /**
     * Encoder de senha usando BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}