package br.tec.facilitaservicos.autenticacao.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.r2dbc.R2dbcRepositoriesAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@Configuration
@EnableAutoConfiguration(exclude = {
    R2dbcAutoConfiguration.class,
    R2dbcDataAutoConfiguration.class,
    R2dbcRepositoriesAutoConfiguration.class
})
@ComponentScan(
    basePackages = "br.tec.facilitaservicos.autenticacao",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
            R2dbcConfiguration.class
        }),
        @ComponentScan.Filter(type = FilterType.ANNOTATION, classes = {
            EnableR2dbcAuditing.class
        })
    }
)
public class TestConfiguration {
    
    @Bean
    @Primary
    public R2dbcMappingContext r2dbcMappingContext() {
        return mock(R2dbcMappingContext.class);
    }
    
    @MockBean
    private R2dbcEntityTemplate r2dbcEntityTemplate;
}