package br.tec.facilitaservicos.autenticacao.configuracao;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * 🛡️ R2DBC URL NORMALIZER - PROGRAMAÇÃO DEFENSIVA 
 * ============================================================================
 * 
 * Componente de programação defensiva que automaticamente converte URLs JDBC
 * para formato R2DBC compatível, evitando falhas em produção.
 * 
 * PROBLEMA RESOLVIDO:
 * - Azure Key Vault pode conter URLs JDBC (jdbc:mysql://)
 * - R2DBC requer URLs no formato r2dbc (r2dbc:mysql://)
 * - Conversão automática e transparente
 * 
 * CONVERSÕES SUPORTADAS:
 * - jdbc:mysql:// -> r2dbc:mysql://
 * - jdbc:postgresql:// -> r2dbc:postgresql://
 * - jdbc:h2:// -> r2dbc:h2://
 * - jdbc:mariadb:// -> r2dbc:mariadb://
 * 
 * @author Sistema de Migração R2DBC
 * @version 1.0
 * @since 2024
 */
@Component
public class R2dbcUrlNormalizer implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(R2dbcUrlNormalizer.class);
    
    // Padrões de conversão JDBC -> R2DBC
    private static final Pattern JDBC_MYSQL_PATTERN = Pattern.compile("^jdbc:mysql://");
    private static final Pattern JDBC_POSTGRESQL_PATTERN = Pattern.compile("^jdbc:postgresql://");
    private static final Pattern JDBC_H2_PATTERN = Pattern.compile("^jdbc:h2://");
    private static final Pattern JDBC_MARIADB_PATTERN = Pattern.compile("^jdbc:mariadb://");
    
    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        
        // Propriedades que precisam ser verificadas e convertidas
        String[] r2dbcProperties = {
            "spring.r2dbc.url",
            "conexao-de-sorte-database-r2dbc-url"
        };
        
        Map<String, Object> normalizedProperties = new HashMap<>();
        boolean hasChanges = false;
        
        for (String property : r2dbcProperties) {
            String originalValue = environment.getProperty(property);
            
            if (originalValue != null && isJdbcUrl(originalValue)) {
                String convertedValue = convertJdbcToR2dbc(originalValue);
                if (!originalValue.equals(convertedValue)) {
                    normalizedProperties.put(property, convertedValue);
                    hasChanges = true;
                    
                    logger.info("🛡️ R2DBC URL Normalizer - Conversão automática aplicada:");
                    logger.info("   Propriedade: {}", property);
                    logger.info("   Original: {}", maskUrl(originalValue));
                    logger.info("   Convertida: {}", maskUrl(convertedValue));
                }
            }
        }
        
        // Adicionar propriedades convertidas se houver mudanças
        if (hasChanges) {
            environment.getPropertySources().addFirst(
                new MapPropertySource("r2dbcUrlNormalizer", normalizedProperties)
            );
            
            logger.info("✅ R2DBC URL Normalizer - {} propriedades convertidas com sucesso", 
                       normalizedProperties.size());
        } else {
            logger.debug("🔍 R2DBC URL Normalizer - Nenhuma conversão necessária");
        }
    }
    
    /**
     * Verifica se a URL é uma URL JDBC que precisa ser convertida
     */
    private boolean isJdbcUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        return url.startsWith("jdbc:") && !url.startsWith("r2dbc:");
    }
    
    /**
     * Converte URL JDBC para formato R2DBC compatível
     */
    private String convertJdbcToR2dbc(String jdbcUrl) {
        if (jdbcUrl == null) {
            return null;
        }
        
        String converted = jdbcUrl;
        
        // MySQL
        if (JDBC_MYSQL_PATTERN.matcher(converted).find()) {
            converted = JDBC_MYSQL_PATTERN.matcher(converted).replaceFirst("r2dbc:mysql://");
        }
        // PostgreSQL  
        else if (JDBC_POSTGRESQL_PATTERN.matcher(converted).find()) {
            converted = JDBC_POSTGRESQL_PATTERN.matcher(converted).replaceFirst("r2dbc:postgresql://");
        }
        // H2
        else if (JDBC_H2_PATTERN.matcher(converted).find()) {
            converted = JDBC_H2_PATTERN.matcher(converted).replaceFirst("r2dbc:h2://");
        }
        // MariaDB
        else if (JDBC_MARIADB_PATTERN.matcher(converted).find()) {
            converted = JDBC_MARIADB_PATTERN.matcher(converted).replaceFirst("r2dbc:mariadb://");
        }
        // Conversão genérica para outros drivers
        else if (converted.startsWith("jdbc:")) {
            converted = converted.replaceFirst("^jdbc:", "r2dbc:");
            logger.warn("⚠️ Conversão genérica aplicada para: {}", maskUrl(jdbcUrl));
            logger.warn("   Verifique se o driver R2DBC está disponível para este banco");
        }
        
        return converted;
    }
    
    /**
     * Mascara informações sensíveis da URL para logs
     */
    private String maskUrl(String url) {
        if (url == null) {
            return "null";
        }
        
        // Mascarar senha se presente: user:password@host -> user:****@host
        return url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
    }
}