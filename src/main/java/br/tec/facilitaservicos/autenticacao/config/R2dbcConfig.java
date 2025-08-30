package br.tec.facilitaservicos.autenticacao.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.lang.NonNull;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

import io.asyncer.r2dbc.mysql.MySqlConnectionConfiguration;
import io.asyncer.r2dbc.mysql.MySqlConnectionFactory;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactory;

/**
 * Configuração do R2DBC para acesso reativo ao banco MySQL.
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "br.tec.facilitaservicos.autenticacao.repository")
public class R2dbcConfig extends AbstractR2dbcConfiguration {
    
    @Value("${spring.r2dbc.url:r2dbc:mysql://localhost:3306/conexao_sorte_auth}")
    private String url;
    
    @Value("${spring.r2dbc.username:conexao_sorte}")
    private String username;
    
    @Value("${spring.r2dbc.password:senha123}")
    private String password;
    
    @Value("${spring.r2dbc.pool.initial-size:5}")
    private int initialSize;
    
    @Value("${spring.r2dbc.pool.max-size:20}")
    private int maxSize;
    
    @Value("${spring.r2dbc.pool.max-idle-time:PT30M}")
    private Duration maxIdleTime;
    
    @Override
    @Bean
    public @NonNull ConnectionFactory connectionFactory() {
        // Parse da URL para extrair host, porta e database
        String cleanUrl = url.replace("r2dbc:mysql://", "");
        String[] urlParts = cleanUrl.split("/");
        String[] hostPort = urlParts[0].split(":");
        
        String host = hostPort[0];
        int port = hostPort.length > 1 ? Integer.parseInt(hostPort[1]) : 3306;
        String database = urlParts.length > 1 ? urlParts[1] : "conexao_sorte_auth";
        
        MySqlConnectionConfiguration configuration = MySqlConnectionConfiguration.builder()
            .host(host)
            .port(port)
            .username(username)
            .password(password)
            .database(database)
            .connectTimeout(Duration.ofSeconds(10))
            // SSL desabilitado para desenvolvimento
            .autodetectExtensions(false)
            .build();
        
        MySqlConnectionFactory connectionFactory = MySqlConnectionFactory.from(configuration);
        
        // Configuração do pool de conexões
        ConnectionPoolConfiguration poolConfiguration = ConnectionPoolConfiguration.builder(connectionFactory)
            .initialSize(initialSize)
            .maxSize(maxSize)
            .maxIdleTime(maxIdleTime)
            .maxAcquireTime(Duration.ofSeconds(60))
            .maxCreateConnectionTime(Duration.ofSeconds(30))
            .validationQuery("SELECT 1")
            .build();
        
        return new ConnectionPool(poolConfiguration);
    }
    
    /**
     * Gerenciador de transações reativo.
     */
    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}