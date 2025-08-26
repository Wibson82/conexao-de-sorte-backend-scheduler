-- ============================================================================
-- MIGRATION V001: Criação da tabela de usuários
-- ============================================================================
-- Descrição: Cria tabela de usuários para autenticação
-- Data: 2025-08-23
-- Autor: Sistema de Autenticação
-- ============================================================================

CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    nome_usuario VARCHAR(100) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    email_verificado BOOLEAN NOT NULL DEFAULT FALSE,
    tentativas_login_falidas INT NOT NULL DEFAULT 0,
    conta_bloqueada BOOLEAN NOT NULL DEFAULT FALSE,
    data_bloqueio DATETIME NULL,
    data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 1,
    
    -- Índices para performance
    INDEX idx_usuarios_email (email),
    INDEX idx_usuarios_nome_usuario (nome_usuario),
    INDEX idx_usuarios_ativo (ativo),
    INDEX idx_usuarios_conta_bloqueada (conta_bloqueada),
    INDEX idx_usuarios_email_verificado (email_verificado),
    INDEX idx_usuarios_data_criacao (data_criacao),
    INDEX idx_usuarios_data_atualizacao (data_atualizacao),
    
    -- Índice composto para login
    INDEX idx_usuarios_login (email, nome_usuario, ativo),
    
    -- Índice para limpeza de dados
    INDEX idx_usuarios_limpeza (ativo, data_atualizacao)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci
  COMMENT='Tabela de usuários do sistema de autenticação';

-- ============================================================================
-- Dados iniciais para desenvolvimento/testes
-- ============================================================================

-- Usuário administrador padrão (senha: admin123)
-- Hash: $2a$12$LQv3c1yqBWVHxkd0LHAkCOaOEjf5JGBEObdh9CU7EhOEhGNj7KYem
INSERT INTO usuarios (
    email, 
    nome_usuario, 
    senha_hash, 
    ativo, 
    email_verificado,
    data_criacao,
    data_atualizacao
) VALUES (
    'admin@conexaodesorte.com',
    'admin',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOaOEjf5JGBEObdh9CU7EhOEhGNj7KYem',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Usuário de teste padrão (senha: teste123)
-- Hash: $2a$12$MemvpwqTM5k/vr4HPQpTgOUKiOE/7zy7w5kCgqUHrE5kxVxaUE4zC
INSERT INTO usuarios (
    email, 
    nome_usuario, 
    senha_hash, 
    ativo, 
    email_verificado,
    data_criacao,
    data_atualizacao
) VALUES (
    'teste@conexaodesorte.com',
    'teste',
    '$2a$12$MemvpwqTM5k/vr4HPQpTgOUKiOE/7zy7w5kCgqUHrE5kxVxaUE4zC',
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- ============================================================================
-- Verificações de integridade
-- ============================================================================

-- Verificar se a tabela foi criada corretamente
SELECT 
    'usuarios' as tabela,
    COUNT(*) as registros,
    COUNT(CASE WHEN ativo = TRUE THEN 1 END) as usuarios_ativos,
    COUNT(CASE WHEN email_verificado = TRUE THEN 1 END) as emails_verificados
FROM usuarios;