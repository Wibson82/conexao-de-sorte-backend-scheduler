package br.tec.facilitaservicos.autenticacao.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Configuração base para testes WebFlux que exclui automaticamente
 * as configurações do R2DBC para evitar problemas de dependência.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@WebFluxTest
@EnableAutoConfiguration(exclude = {
    R2dbcAutoConfiguration.class,
    R2dbcDataAutoConfiguration.class,
    R2dbcRepositoriesAutoConfiguration.class
})

@TestPropertySource(properties = {
    "spring.r2dbc.enabled=false",
    "spring.cache.type=simple",
    "spring.cache.cache-names=jwks"
})
public @interface BaseWebFluxTest {
    /**
     * Especifica os controllers a serem testados.
     */
    Class<?>[] controllers() default {};
}