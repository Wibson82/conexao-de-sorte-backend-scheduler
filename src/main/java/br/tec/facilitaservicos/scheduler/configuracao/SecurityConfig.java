package br.tec.facilitaservicos.scheduler.configuracao;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Configuração de segurança para o microserviço Scheduler.
 * Configura OIDC JWT Resource Server.
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Actuator endpoints públicos
                .pathMatchers("/actuator/health", "/actuator/prometheus").permitAll()
                // Todos os outros endpoints requerem autenticação
                .pathMatchers("/jobs/**", "/diagnostico/**").hasAuthority("SCOPE_scheduler.write")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtAuthenticationConverter(jwtAuthenticationToken -> {
                        // Converter authorities dos scopes JWT
                        return jwtAuthenticationToken;
                    })
                )
            )
            .build();
    }
}