package br.tec.facilitaservicos.autenticacao.config;

import br.tec.facilitaservicos.autenticacao.service.AuthService;
import br.tec.facilitaservicos.autenticacao.repository.RefreshTokenRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.Mockito.mock;

/**
 * Configuração personalizada para testes WebFlux que exclui completamente
 * todas as configurações relacionadas ao R2DBC, incluindo auditoria e repositórios.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    R2dbcAutoConfiguration.class,
    R2dbcDataAutoConfiguration.class,
    R2dbcRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class
})
@ComponentScan(
    basePackages = "br.tec.facilitaservicos.autenticacao",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            R2dbcConfiguration.class
        })
    }
)
public class WebFluxTestConfiguration {

    @Bean
    public AuthService authService() {
        return mock(AuthService.class);
    }

    @Bean
    public RefreshTokenRepository refreshTokenRepository() {
        return mock(RefreshTokenRepository.class);
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate() {
        return mock(R2dbcEntityTemplate.class);
    }

    @Bean
    public R2dbcMappingContext r2dbcMappingContext() {
        return mock(R2dbcMappingContext.class);
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}