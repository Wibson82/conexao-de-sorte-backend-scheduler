-- ============================================================================
-- MIGRATION V002: Criação da tabela de refresh tokens
-- ============================================================================
-- Descrição: Cria tabela para gerenciar refresh tokens de autenticação
-- Data: 2025-08-23
-- Autor: Sistema de Autenticação
-- ============================================================================

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    usuario_id BIGINT NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    revogado BOOLEAN NOT NULL DEFAULT FALSE,
    data_expiracao DATETIME NOT NULL,
    ip_origem VARCHAR(45) NULL COMMENT 'IP de origem da requisição (suporta IPv6)',
    user_agent TEXT NULL COMMENT 'User-Agent do cliente',
    familia_token VARCHAR(36) NOT NULL COMMENT 'UUID para agrupar tokens da mesma família',
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 1,
    
    -- Chave estrangeira para usuários
    CONSTRAINT fk_refresh_tokens_usuario 
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id) 
        ON DELETE CASCADE 
        ON UPDATE CASCADE,
    
    -- Índices para performance
    INDEX idx_refresh_tokens_token_hash (token_hash),
    INDEX idx_refresh_tokens_usuario_id (usuario_id),
    INDEX idx_refresh_tokens_ativo (ativo),
    INDEX idx_refresh_tokens_revogado (revogado),
    INDEX idx_refresh_tokens_data_expiracao (data_expiracao),
    INDEX idx_refresh_tokens_familia_token (familia_token),
    INDEX idx_refresh_tokens_ip_origem (ip_origem),
    INDEX idx_refresh_tokens_data_criacao (data_criacao),
    
    -- Índices compostos para consultas frequentes
    INDEX idx_refresh_tokens_valido (usuario_id, ativo, revogado, data_expiracao),
    INDEX idx_refresh_tokens_usuario_ativo (usuario_id, ativo, revogado),
    INDEX idx_refresh_tokens_limpeza (data_expiracao, revogado),
    INDEX idx_refresh_tokens_seguranca (ip_origem, data_criacao, ativo),
    
    -- Índice para auditoria e análise de segurança
    INDEX idx_refresh_tokens_auditoria (usuario_id, ip_origem, data_criacao, ativo)
    
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Tabela de refresh tokens para autenticação';

-- ============================================================================
-- Configurações de limpeza automática
-- ============================================================================

-- Particionamento por data (opcional - para tabelas com muitos tokens)
-- ALTER TABLE refresh_tokens PARTITION BY RANGE (YEAR(data_criacao)) (
--     PARTITION p2024 VALUES LESS THAN (2025),
--     PARTITION p2025 VALUES LESS THAN (2026),
--     PARTITION p2026 VALUES LESS THAN (2027),
--     PARTITION pmax VALUES LESS THAN MAXVALUE
-- );

-- ============================================================================
-- Procedures para limpeza automática (opcional)
-- ============================================================================

-- Procedure para limpeza de tokens expirados
DELIMITER //
CREATE PROCEDURE CleanupExpiredTokens()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE total_deleted INT DEFAULT 0;
    DECLARE batch_deleted INT;
    
    -- Cleanup em batches para evitar lock longo
    REPEAT
        DELETE FROM refresh_tokens 
        WHERE data_expiracao < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 7 DAY)
        LIMIT batch_size;
        
        SET batch_deleted = ROW_COUNT();
        SET total_deleted = total_deleted + batch_deleted;
        
        -- Pequena pausa entre batches
        SELECT SLEEP(0.1);
        
    UNTIL batch_deleted = 0 END REPEAT;
    
    SELECT CONCAT('Tokens expirados removidos: ', total_deleted) as resultado;
END //
DELIMITER ;

-- Procedure para limpeza de tokens revogados antigos
DELIMITER //
CREATE PROCEDURE CleanupRevokedTokens()
BEGIN
    DELETE FROM refresh_tokens 
    WHERE revogado = TRUE 
    AND data_atualizacao < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 30 DAY);
    
    SELECT CONCAT('Tokens revogados antigos removidos: ', ROW_COUNT()) as resultado;
END //
DELIMITER ;

-- ============================================================================
-- Triggers para auditoria e segurança
-- ============================================================================

-- Trigger para log de criação de tokens (opcional)
DELIMITER //
CREATE TRIGGER tr_refresh_token_created
AFTER INSERT ON refresh_tokens
FOR EACH ROW
BEGIN
    -- Aqui poderia inserir em uma tabela de auditoria
    -- INSERT INTO token_audit_log (acao, token_id, usuario_id, ip_origem, data_acao)
    -- VALUES ('CREATED', NEW.id, NEW.usuario_id, NEW.ip_origem, CURRENT_TIMESTAMP);
    
    -- Por enquanto, apenas um comentário para indicar onde seria implementado
    SET @audit_action = 'TOKEN_CREATED';
END //
DELIMITER ;

-- ============================================================================
-- Views para consultas frequentes
-- ============================================================================

-- View para tokens ativos
CREATE VIEW v_tokens_ativos AS
SELECT 
    rt.*,
    u.email,
    u.nome_usuario,
    CASE 
        WHEN rt.data_expiracao < CURRENT_TIMESTAMP THEN 'EXPIRADO'
        WHEN rt.revogado = TRUE THEN 'REVOGADO'
        WHEN rt.ativo = FALSE THEN 'INATIVO'
        ELSE 'ATIVO'
    END as status_token
FROM refresh_tokens rt
INNER JOIN usuarios u ON rt.usuario_id = u.id
WHERE rt.ativo = TRUE AND rt.revogado = FALSE;

-- View para estatísticas de tokens por usuário
CREATE VIEW v_token_stats_usuario AS
SELECT 
    u.id as usuario_id,
    u.email,
    u.nome_usuario,
    COUNT(rt.id) as total_tokens,
    COUNT(CASE WHEN rt.ativo = TRUE AND rt.revogado = FALSE THEN 1 END) as tokens_ativos,
    COUNT(CASE WHEN rt.data_expiracao > CURRENT_TIMESTAMP AND rt.ativo = TRUE AND rt.revogado = FALSE THEN 1 END) as tokens_validos,
    MAX(rt.data_criacao) as ultimo_token_criado
FROM usuarios u
LEFT JOIN refresh_tokens rt ON u.id = rt.usuario_id
GROUP BY u.id, u.email, u.nome_usuario;

-- ============================================================================
-- Verificações de integridade
-- ============================================================================

-- Verificar se a tabela foi criada corretamente
SELECT 
    'refresh_tokens' as tabela,
    COUNT(*) as registros,
    COUNT(CASE WHEN ativo = TRUE THEN 1 END) as tokens_ativos,
    COUNT(CASE WHEN revogado = TRUE THEN 1 END) as tokens_revogados,
    COUNT(CASE WHEN data_expiracao < CURRENT_TIMESTAMP THEN 1 END) as tokens_expirados
FROM refresh_tokens;