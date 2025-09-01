package com.conexaodesorte.autenticacao.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Configuração de teste personalizada para WebFlux que exclui completamente
 * todas as configurações relacionadas ao R2DBC, incluindo auditoria.
 * 
 * Esta configuração resolve problemas com r2dbcAuditingHandler em testes
 * que não precisam de funcionalidades de banco de dados.
 */
@TestConfiguration
@EnableAutoConfiguration(exclude = {
    R2dbcAutoConfiguration.class,
    R2dbcDataAutoConfiguration.class,
    R2dbcRepositoriesAutoConfiguration.class,
    org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration.class
})
@ComponentScan(
    basePackages = "com.conexaodesorte.autenticacao",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            classes = {EnableR2dbcAuditing.class, EnableR2dbcRepositories.class}
        ),
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ".*Repository.*"
        )
    }
)
public class WebFluxTestConfiguration {
    // Esta classe fornece uma configuração limpa para testes WebFlux
    // sem dependências do R2DBC
}