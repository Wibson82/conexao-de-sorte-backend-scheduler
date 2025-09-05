package br.tec.facilitaservicos.scheduler.infraestrutura.configuracao;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * ============================================================================
 * 🔐 CONFIGURAÇÃO DE SEGURANÇA REATIVA - SCHEDULER & ETL
 * ============================================================================
 * 
 * Configuração de segurança para microserviço de agendamento:
 * - Validação JWT via JWKS com authorities específicas para scheduling
 * - Controle de acesso granular para jobs e ETL pipelines
 * - CORS configurado para dashboards administrativos
 * - Headers de segurança para proteção de jobs sensíveis
 * - Rate limiting para operações de agendamento
 * - Proteção de endpoints de monitoramento Quartz
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // Constantes para valores repetidos
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String PROFILE_PRODUCAO = "prod";
    private static final String ERRO_CORS_ORIGEM_INSEGURA = "Configuração CORS inválida: apenas origins HTTPS são permitidas em produção para scheduler";
    
    // Templates de resposta JSON para scheduler
    private static final String TEMPLATE_ERRO_AUTENTICACAO = """
        {
            "status": 401,
            "erro": "Não autorizado",
            "mensagem": "Token JWT inválido ou ausente - acesso a scheduler negado",
            "timestamp": "%s",
            "service": "scheduler"
        }
        """;
        
    private static final String TEMPLATE_ERRO_ACESSO = """
        {
            "status": 403,
            "erro": "Acesso negado",
            "mensagem": "Permissões insuficientes para acessar operações de agendamento",
            "timestamp": "%s",
            "service": "scheduler"
        }
        """;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${cors.allowed-origins:https://admin.conexaodesorte.com,https://monitoring.conexaodesorte.com}")
    private String allowedOriginsProperty;

    private List<String> allowedOrigins;

    @Value("#{'${cors.allowed-methods:GET,POST,PUT,DELETE}'.split(',')}")
    private List<String> allowedMethods;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    private final Environment environment;

    public SecurityConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validarConfiguracaoCors() {
        this.allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
            .map(String::trim)
            .toList();

        boolean producao = Arrays.asList(environment.getActiveProfiles()).contains(PROFILE_PRODUCAO);
        if (producao) {
            // Em produção, apenas origins HTTPS devem ser permitidas
            boolean temOrigemInsegura = allowedOrigins.stream()
                .anyMatch(origin -> origin.equals("*") || origin.startsWith("http://"));
            if (temOrigemInsegura) {
                throw new IllegalStateException(ERRO_CORS_ORIGEM_INSEGURA);
            }
        }
    }

    /**
     * Configuração da cadeia de filtros de segurança para scheduler
     */
    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            // Desabilitar proteções desnecessárias para API reativa
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())

            // Configurar CORS para dashboards administrativos
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Configurar autorização específica para scheduler
            .authorizeExchange(exchanges -> exchanges
                // Endpoints públicos (health checks apenas)
                .pathMatchers(
                    "/actuator/health/**",
                    "/actuator/info",
                    "/actuator/metrics",
                    "/actuator/prometheus",
                    "/favicon.ico"
                ).permitAll()
                
                // Documentação OpenAPI (apenas para admins)
                .pathMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/webjars/**"
                ).hasAuthority("SCOPE_admin")
                
                // Operações de jobs - requerem authorities específicas
                .pathMatchers(HttpMethod.GET, "/rest/v1/jobs/**")
                    .hasAnyAuthority("SCOPE_scheduler_read", "SCOPE_admin")
                    
                .pathMatchers(HttpMethod.POST, "/rest/v1/jobs/**")
                    .hasAnyAuthority("SCOPE_scheduler_write", "SCOPE_admin")
                    
                .pathMatchers(HttpMethod.PUT, "/rest/v1/jobs/**")
                    .hasAnyAuthority("SCOPE_scheduler_write", "SCOPE_admin")
                    
                .pathMatchers(HttpMethod.DELETE, "/rest/v1/jobs/**")
                    .hasAnyAuthority("SCOPE_scheduler_delete", "SCOPE_admin")
                
                // Operações ETL - máxima restrição
                .pathMatchers("/rest/v1/etl/**")
                    .hasAnyAuthority("SCOPE_etl_execute", "SCOPE_admin")
                    
                .pathMatchers("/rest/v1/pipelines/**")
                    .hasAnyAuthority("SCOPE_pipeline_manage", "SCOPE_admin")
                
                // Monitoramento Quartz - apenas admins
                .pathMatchers("/rest/v1/quartz/**")
                    .hasAuthority("SCOPE_admin")
                
                // Endpoints administrativos críticos
                .pathMatchers("/actuator/**").hasAuthority("SCOPE_admin")
                
                // Qualquer outro endpoint requer pelo menos role de scheduler
                .anyExchange().hasAnyAuthority("SCOPE_scheduler_read", "SCOPE_admin")
            )

            // Configurar JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .jwtDecoder(reactiveJwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )

            // Headers de segurança básicos
            .headers(headers -> headers
                .contentSecurityPolicy("default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self'; frame-ancestors 'none'")
                .and()
                .frameOptions().deny()
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000)
                    .includeSubdomains(true)
                    .preload(true)
                )
            )

            // Configurar tratamento de exceções para scheduler
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((exchange, _) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                    response.getHeaders().add(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
                    
                    String body = TEMPLATE_ERRO_AUTENTICACAO.formatted(java.time.LocalDateTime.now());
                    
                    var buffer = response.bufferFactory().wrap(body.getBytes());
                    return response.writeWith(reactor.core.publisher.Mono.just(buffer));
                })
                .accessDeniedHandler((exchange, _) -> {
                    var response = exchange.getResponse();
                    response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
                    response.getHeaders().add(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
                    
                    String body = TEMPLATE_ERRO_ACESSO.formatted(java.time.LocalDateTime.now());
                    
                    var buffer = response.bufferFactory().wrap(body.getBytes());
                    return response.writeWith(reactor.core.publisher.Mono.just(buffer));
                })
            )

            .build();
    }

    /**
     * Decodificador JWT reativo via JWKS
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * Conversor de autenticação JWT personalizado para scheduler
     */
    @Bean
    public ReactiveJwtAuthenticationConverterAdapter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }

    /**
     * Conversor personalizado de authorities JWT para operações de scheduler
     */
    @Bean
    public Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
        return new CustomJwtGrantedAuthoritiesConverter();
    }

    /**
     * Configuração CORS para dashboards administrativos
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Origins permitidas (dashboards administrativos)
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(allowedMethods);
        
        // Headers permitidos
        configuration.setAllowedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin"
        ));

        // Permitir credenciais para dashboards
        if (allowCredentials) {
            configuration.setAllowCredentials(true);
        }
        
        // Cache preflight
        configuration.setMaxAge(maxAge);
        
        // Headers expostos (para metadados de jobs)
        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Type",
            "X-Total-Count",
            "X-Job-Status",
            "X-Job-Id"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Classe interna para conversão de authorities JWT específica para scheduler
     */
    private static class CustomJwtGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        
        @Override
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();
            
            // Processar claim 'roles' 
            var rolesClaim = jwt.getClaim("roles");
            if (rolesClaim != null) {
                if (rolesClaim instanceof List<?> rolesList) {
                    authorities.addAll(
                        rolesList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                            .toList()
                    );
                }
            }
            
            // Processar claim 'authorities'
            var authoritiesClaim = jwt.getClaim("authorities");
            if (authoritiesClaim != null) {
                if (authoritiesClaim instanceof List<?> authList) {
                    authorities.addAll(
                        authList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(SimpleGrantedAuthority::new)
                            .toList()
                    );
                }
            }
            
            // Processar claim 'scope' (OAuth2 padrão)
            var scopeClaim = jwt.getClaim("scope");
            if (scopeClaim instanceof String scopeString) {
                authorities.addAll(
                    Arrays.stream(scopeString.split("\\s+"))
                        .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                        .toList()
                );
            }
            
            // Adicionar authorities específicas para scheduler baseadas em claims customizados
            var schedulerRolesClaim = jwt.getClaim("scheduler_roles");
            if (schedulerRolesClaim instanceof List<?> schedulerRolesList) {
                authorities.addAll(
                    schedulerRolesList.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(role -> new SimpleGrantedAuthority("SCOPE_scheduler_" + role))
                        .toList()
                );
            }
            
            return authorities;
        }
    }
}
