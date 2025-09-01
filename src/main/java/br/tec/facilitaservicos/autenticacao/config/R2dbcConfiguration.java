package br.tec.facilitaservicos.autenticacao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Configuração do R2DBC para repositórios e auditoria.
 * 
 * Esta configuração foi separada da classe principal da aplicação
 * para evitar problemas com testes @WebFluxTest que não precisam
 * de configurações de banco de dados.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "br.tec.facilitaservicos.autenticacao.repository")
@EnableR2dbcAuditing
public class R2dbcConfiguration {
    // Configuração vazia - as funcionalidades são habilitadas pelas anotações
}