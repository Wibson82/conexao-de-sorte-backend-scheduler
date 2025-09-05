-- ============================================================================
-- 🗄️ SCHEMA DO MICROSERVIÇO SCHEDULER
-- ============================================================================
-- Scripts SQL para criação das tabelas necessárias para o microserviço
-- de scheduler incluindo tabelas do Quartz e tabelas customizadas.
-- 
-- Suporte: MySQL 8.4+
-- Charset: utf8mb4 para suporte completo a emojis e caracteres especiais
-- Engine: InnoDB para suporte a transações e integridade referencial
-- ============================================================================

-- === CONFIGURAÇÕES INICIAIS ===
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";

-- === CRIAÇÃO DO BANCO DE DADOS ===
CREATE DATABASE IF NOT EXISTS `conexao_sorte_scheduler` 
    DEFAULT CHARACTER SET utf8mb4 
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `conexao_sorte_scheduler`;

-- === TABELA PRINCIPAL DE JOBS ===
CREATE TABLE IF NOT EXISTS `jobs` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY COMMENT 'ID único do job (UUID)',
    `nome` VARCHAR(100) NOT NULL COMMENT 'Nome do job',
    `descricao` TEXT COMMENT 'Descrição detalhada do job',
    `tipo` ENUM('ETL_RESULTADOS','ETL_GENERICO','ETL_SINCRONIZACAO','BATCH_PROCESSAMENTO','BATCH_BACKUP','BATCH_CLEANUP','WEBHOOK','WEBHOOK_NOTIFICATION','WEBHOOK_CALLBACK','MONITORING_HEALTH','MONITORING_METRICS','MONITORING_SLA','MAINTENANCE_DB','MAINTENANCE_CACHE','MAINTENANCE_LOGS','NOTIFICATION_SCHEDULED','NOTIFICATION_DIGEST','NOTIFICATION_REMINDER','REPORT_EXECUTIVE','REPORT_OPERATIONAL','REPORT_COMPLIANCE','CUSTOM') NOT NULL COMMENT 'Tipo do job',
    `status` ENUM('CRIADO','PRONTO','AGENDADO','EXECUTANDO','EXECUTADO','COMPLETADO','FALHADO','TIMEOUT','CANCELADO','CIRCUIT_BREAKER_ABERTO','PAUSADO','DESATIVADO','ARQUIVADO','AGUARDANDO_DEPENDENCIAS','BLOQUEADO','AGUARDANDO_SLOT','REEXECUTANDO','INTERROMPIDO','PROCESSANDO') NOT NULL DEFAULT 'CRIADO' COMMENT 'Status atual do job',
    `prioridade` ENUM('EMERGENCIA','CRITICA','ALTA','NORMAL','BAIXA') NOT NULL DEFAULT 'NORMAL' COMMENT 'Prioridade do job',
    `prioridade_peso` INT NOT NULL DEFAULT 40 COMMENT 'Peso numérico da prioridade para ordenação',
    `grupo` VARCHAR(50) NOT NULL DEFAULT 'DEFAULT' COMMENT 'Grupo do job',
    `cron_expression` VARCHAR(50) COMMENT 'Expressão CRON para agendamento',
    `parametros` JSON COMMENT 'Parâmetros do job em formato JSON',
    `timeout_segundos` INT NOT NULL DEFAULT 300 COMMENT 'Timeout em segundos',
    `max_tentativas` INT NOT NULL DEFAULT 3 COMMENT 'Máximo de tentativas',
    `tentativas` INT NOT NULL DEFAULT 0 COMMENT 'Tentativas já realizadas',
    `max_execucoes_concorrentes` INT DEFAULT 1 COMMENT 'Máximo de execuções concorrentes',
    `execucoes_ativas` INT NOT NULL DEFAULT 0 COMMENT 'Execuções atualmente ativas',
    `total_execucoes` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total de execuções',
    `total_sucessos` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total de sucessos',
    `total_falhas` BIGINT NOT NULL DEFAULT 0 COMMENT 'Total de falhas',
    `ultimo_erro` TEXT COMMENT 'Última mensagem de erro',
    `duracao_execucao_ms` BIGINT COMMENT 'Duração da última execução em ms',
    `circuit_breaker_aberto` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Se o circuit breaker está aberto',
    `circuit_breaker_failures` INT NOT NULL DEFAULT 0 COMMENT 'Número de falhas consecutivas',
    `circuit_breaker_aberto_em` TIMESTAMP NULL COMMENT 'Quando o circuit breaker foi aberto',
    `agendado_para` TIMESTAMP NULL COMMENT 'Data/hora para execução agendada',
    `iniciado_em` TIMESTAMP NULL COMMENT 'Quando foi iniciado',
    `completado_em` TIMESTAMP NULL COMMENT 'Quando foi completado',
    `proximo_retry` TIMESTAMP NULL COMMENT 'Data/hora do próximo retry',
    `timeout_em` TIMESTAMP NULL COMMENT 'Data/hora limite para timeout',
    `ativo` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Se o job está ativo',
    `criado_em` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data de criação',
    `atualizado_em` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Data de atualização',
    
    -- Índices
    INDEX `idx_status` (`status`),
    INDEX `idx_tipo` (`tipo`),
    INDEX `idx_prioridade_peso` (`prioridade_peso` DESC),
    INDEX `idx_grupo` (`grupo`),
    INDEX `idx_agendado_para` (`agendado_para`),
    INDEX `idx_proximo_retry` (`proximo_retry`),
    INDEX `idx_timeout_em` (`timeout_em`),
    INDEX `idx_circuit_breaker` (`circuit_breaker_aberto`),
    INDEX `idx_ativo` (`ativo`),
    INDEX `idx_criado_em` (`criado_em`),
    INDEX `idx_status_prioridade` (`status`, `prioridade_peso` DESC),
    INDEX `idx_pronto_execucao` (`status`, `agendado_para`, `circuit_breaker_aberto`),
    INDEX `idx_retry` (`status`, `tentativas`, `max_tentativas`, `proximo_retry`),
    
    -- Índice composto para queries de listagem
    INDEX `idx_busca_geral` (`ativo`, `status`, `tipo`, `grupo`, `criado_em`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='Tabela principal de jobs do scheduler';

-- === TABELA DE DEPENDÊNCIAS ENTRE JOBS ===
CREATE TABLE IF NOT EXISTS `job_dependencias` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_id` VARCHAR(36) NOT NULL COMMENT 'ID do job que depende',
    `dependencia_id` VARCHAR(36) NOT NULL COMMENT 'ID do job dependência',
    `tipo_dependencia` ENUM('SUCESSO','COMPLETADO','QUALQUER') NOT NULL DEFAULT 'SUCESSO' COMMENT 'Tipo de dependência',
    `criado_em` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT `fk_job_dependencias_job` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_job_dependencias_dep` FOREIGN KEY (`dependencia_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE,
    UNIQUE KEY `uk_job_dependencia` (`job_id`, `dependencia_id`),
    INDEX `idx_dependencias_job` (`job_id`),
    INDEX `idx_dependencias_dep` (`dependencia_id`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='Dependências entre jobs';

-- === TABELA DE HISTÓRICO DE EXECUÇÕES ===
CREATE TABLE IF NOT EXISTS `job_execucoes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_id` VARCHAR(36) NOT NULL COMMENT 'ID do job',
    `tentativa` INT NOT NULL COMMENT 'Número da tentativa',
    `status` ENUM('EXECUTANDO','COMPLETADO','FALHADO','TIMEOUT','CANCELADO') NOT NULL COMMENT 'Status da execução',
    `iniciado_em` TIMESTAMP NOT NULL COMMENT 'Quando foi iniciado',
    `completado_em` TIMESTAMP NULL COMMENT 'Quando foi completado',
    `duracao_ms` BIGINT COMMENT 'Duração em milissegundos',
    `erro` TEXT COMMENT 'Mensagem de erro se houver',
    `resultado` JSON COMMENT 'Resultado da execução',
    `host_execucao` VARCHAR(255) COMMENT 'Host onde foi executado',
    `thread_execucao` VARCHAR(100) COMMENT 'Thread de execução',
    
    CONSTRAINT `fk_job_execucoes_job` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE,
    INDEX `idx_execucoes_job` (`job_id`),
    INDEX `idx_execucoes_status` (`status`),
    INDEX `idx_execucoes_data` (`iniciado_em`),
    INDEX `idx_execucoes_duracao` (`duracao_ms`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='Histórico de execuções dos jobs';

-- === TABELA DE MÉTRICAS DE JOBS ===
CREATE TABLE IF NOT EXISTS `job_metricas` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `job_id` VARCHAR(36) NOT NULL COMMENT 'ID do job',
    `data_hora` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Data/hora da métrica',
    `tipo_metrica` VARCHAR(50) NOT NULL COMMENT 'Tipo da métrica',
    `valor` DECIMAL(15,4) NOT NULL COMMENT 'Valor da métrica',
    `unidade` VARCHAR(20) COMMENT 'Unidade da métrica',
    `tags` JSON COMMENT 'Tags adicionais',
    
    CONSTRAINT `fk_job_metricas_job` FOREIGN KEY (`job_id`) REFERENCES `jobs` (`id`) ON DELETE CASCADE,
    INDEX `idx_metricas_job` (`job_id`),
    INDEX `idx_metricas_tipo` (`tipo_metrica`),
    INDEX `idx_metricas_data` (`data_hora`),
    INDEX `idx_metricas_busca` (`job_id`, `tipo_metrica`, `data_hora`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci 
  COMMENT='Métricas de performance dos jobs';

-- ============================================================================
-- 📋 TABELAS DO QUARTZ SCHEDULER
-- ============================================================================
-- Tabelas necessárias para o funcionamento do Quartz Scheduler em cluster
-- com persistência MySQL. Baseado na versão oficial do Quartz.

-- === TABELA DE DETALHES DE JOBS ===
CREATE TABLE IF NOT EXISTS `qrtz_job_details` (
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `JOB_NAME` VARCHAR(200) NOT NULL,
    `JOB_GROUP` VARCHAR(200) NOT NULL,
    `DESCRIPTION` VARCHAR(250) NULL,
    `JOB_CLASS_NAME` VARCHAR(250) NOT NULL,
    `IS_DURABLE` BOOLEAN NOT NULL,
    `IS_NONCONCURRENT` BOOLEAN NOT NULL,
    `IS_UPDATE_DATA` BOOLEAN NOT NULL,
    `REQUESTS_RECOVERY` BOOLEAN NOT NULL,
    `JOB_DATA` BLOB NULL,
    PRIMARY KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`),
    INDEX `idx_qrtz_j_req_recovery` (`SCHED_NAME`, `REQUESTS_RECOVERY`),
    INDEX `idx_qrtz_j_grp` (`SCHED_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- === TABELA DE TRIGGERS ===
CREATE TABLE IF NOT EXISTS `qrtz_triggers` (
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `TRIGGER_NAME` VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    `JOB_NAME` VARCHAR(200) NOT NULL,
    `JOB_GROUP` VARCHAR(200) NOT NULL,
    `DESCRIPTION` VARCHAR(250) NULL,
    `NEXT_FIRE_TIME` BIGINT(19) NULL,
    `PREV_FIRE_TIME` BIGINT(19) NULL,
    `PRIORITY` INTEGER NULL,
    `TRIGGER_STATE` VARCHAR(16) NOT NULL,
    `TRIGGER_TYPE` VARCHAR(8) NOT NULL,
    `START_TIME` BIGINT(19) NOT NULL,
    `END_TIME` BIGINT(19) NULL,
    `CALENDAR_NAME` VARCHAR(200) NULL,
    `MISFIRE_INSTR` SMALLINT NULL,
    `JOB_DATA` BLOB NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    INDEX `idx_qrtz_t_j` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`),
    INDEX `idx_qrtz_t_jg` (`SCHED_NAME`, `JOB_GROUP`),
    INDEX `idx_qrtz_t_c` (`SCHED_NAME`, `CALENDAR_NAME`),
    INDEX `idx_qrtz_t_g` (`SCHED_NAME`, `TRIGGER_GROUP`),
    INDEX `idx_qrtz_t_state` (`SCHED_NAME`, `TRIGGER_STATE`),
    INDEX `idx_qrtz_t_n_state` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`, `TRIGGER_STATE`),
    INDEX `idx_qrtz_t_n_g_state` (`SCHED_NAME`, `TRIGGER_GROUP`, `TRIGGER_STATE`),
    INDEX `idx_qrtz_t_next_fire_time` (`SCHED_NAME`, `NEXT_FIRE_TIME`),
    INDEX `idx_qrtz_t_nft_st` (`SCHED_NAME`, `TRIGGER_STATE`, `NEXT_FIRE_TIME`),
    INDEX `idx_qrtz_t_nft_misfire` (`SCHED_NAME`, `MISFIRE_INSTR`, `NEXT_FIRE_TIME`),
    INDEX `idx_qrtz_t_nft_st_misfire` (`SCHED_NAME`, `MISFIRE_INSTR`, `NEXT_FIRE_TIME`, `TRIGGER_STATE`),
    INDEX `idx_qrtz_t_nft_st_misfire_grp` (`SCHED_NAME`, `MISFIRE_INSTR`, `NEXT_FIRE_TIME`, `TRIGGER_GROUP`, `TRIGGER_STATE`),
    CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`) REFERENCES `qrtz_job_details` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- === TABELA DE TRIGGERS SIMPLES ===
CREATE TABLE IF NOT EXISTS `qrtz_simple_triggers` (
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `TRIGGER_NAME` VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    `REPEAT_COUNT` BIGINT(7) NOT NULL,
    `REPEAT_INTERVAL` BIGINT(12) NOT NULL,
    `TIMES_TRIGGERED` BIGINT(10) NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- === TABELA DE TRIGGERS CRON ===
CREATE TABLE IF NOT EXISTS `qrtz_cron_triggers` (
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `TRIGGER_NAME` VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    `CRON_EXPRESSION` VARCHAR(120) NOT NULL,
    `TIME_ZONE_ID` VARCHAR(80),
    PRIMARY KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- === TABELA DE TRIGGERS DISPARADOS ===
CREATE TABLE IF NOT EXISTS `qrtz_fired_triggers` (
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `ENTRY_ID` VARCHAR(95) NOT NULL,
    `TRIGGER_NAME` VARCHAR(200) NOT NULL,
    `TRIGGER_GROUP` VARCHAR(200) NOT NULL,
    `INSTANCE_NAME` VARCHAR(200) NOT NULL,
    `FIRED_TIME` BIGINT(19) NOT NULL,
    `SCHED_TIME` BIGINT(19) NOT NULL,
    `PRIORITY` INTEGER NOT NULL,
    `STATE` VARCHAR(16) NOT NULL,
    `JOB_NAME` VARCHAR(200) NULL,
    `JOB_GROUP` VARCHAR(200) NULL,
    `IS_NONCONCURRENT` BOOLEAN NULL,
    `REQUESTS_RECOVERY` BOOLEAN NULL,
    PRIMARY KEY (`SCHED_NAME`, `ENTRY_ID`),
    INDEX `idx_qrtz_ft_trig_inst_name` (`SCHED_NAME`, `INSTANCE_NAME`),
    INDEX `idx_qrtz_ft_inst_job_req_rcvry` (`SCHED_NAME`, `INSTANCE_NAME`, `REQUESTS_RECOVERY`),
    INDEX `idx_qrtz_ft_j_g` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`),
    INDEX `idx_qrtz_ft_jg` (`SCHED_NAME`, `JOB_GROUP`),
    INDEX `idx_qrtz_ft_t_g` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`),
    INDEX `idx_qrtz_ft_tg` (`SCHED_NAME`, `TRIGGER_GROUP`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- === TABELA DE CALENDÁRIOS ===
CREATE TABLE IF NOT EXISTS `qrtz_calendars` (
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `CALENDAR_NAME` VARCHAR(200) NOT NULL,
    `CALENDAR` BLOB NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `CALENDAR_NAME`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- === TABELA DE ESTADO DO SCHEDULER ===
CREATE TABLE IF NOT EXISTS `qrtz_scheduler_state` (
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `INSTANCE_NAME` VARCHAR(200) NOT NULL,
    `LAST_CHECKIN_TIME` BIGINT(19) NOT NULL,
    `CHECKIN_INTERVAL` BIGINT(19) NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `INSTANCE_NAME`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- === TABELA DE LOCKS ===
CREATE TABLE IF NOT EXISTS `qrtz_locks` (
    `SCHED_NAME` VARCHAR(120) NOT NULL,
    `LOCK_NAME` VARCHAR(40) NOT NULL,
    PRIMARY KEY (`SCHED_NAME`, `LOCK_NAME`)
) ENGINE=InnoDB 
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_unicode_ci;

-- === INSERÇÃO DE DADOS INICIAIS ===

-- Locks necessários para o Quartz
INSERT INTO `qrtz_locks` VALUES('ConexaoSorteScheduler', 'CALENDAR_ACCESS');
INSERT INTO `qrtz_locks` VALUES('ConexaoSorteScheduler', 'JOB_ACCESS');
INSERT INTO `qrtz_locks` VALUES('ConexaoSorteScheduler', 'MISFIRE_ACCESS');
INSERT INTO `qrtz_locks` VALUES('ConexaoSorteScheduler', 'STATE_ACCESS');
INSERT INTO `qrtz_locks` VALUES('ConexaoSorteScheduler', 'TRIGGER_ACCESS');

-- === JOBS INICIAIS DE EXEMPLO ===
INSERT INTO `jobs` (
    `id`, `nome`, `descricao`, `tipo`, `prioridade`, `prioridade_peso`, 
    `grupo`, `cron_expression`, `timeout_segundos`, `max_tentativas`
) VALUES 
(
    UUID(), 
    'ETL Mega-Sena', 
    'Extração automática de resultados da Mega-Sena', 
    'ETL_RESULTADOS', 
    'ALTA', 
    60,
    'LOTERIAS', 
    '0 0 */2 * * ?', 
    600, 
    5
),
(
    UUID(), 
    'Health Check Diário', 
    'Verificação diária da saúde do sistema', 
    'MONITORING_HEALTH', 
    'NORMAL', 
    40,
    'MONITORING', 
    '0 0 6 * * ?', 
    120, 
    3
),
(
    UUID(), 
    'Backup Diário', 
    'Backup automático dos dados críticos', 
    'BATCH_BACKUP', 
    'ALTA', 
    60,
    'BACKUP', 
    '0 0 2 * * ?', 
    3600, 
    2
),
(
    UUID(), 
    'Limpeza de Logs', 
    'Limpeza automática de logs antigos', 
    'MAINTENANCE_LOGS', 
    'BAIXA', 
    20,
    'MAINTENANCE', 
    '0 0 4 * * ?', 
    900, 
    2
);

-- Finalizar transação
COMMIT;

-- === VIEWS ÚTEIS PARA MONITORAMENTO ===

-- View para estatísticas de jobs
CREATE OR REPLACE VIEW `v_job_estatisticas` AS
SELECT 
    DATE(criado_em) as data,
    tipo,
    status,
    COUNT(*) as quantidade,
    AVG(duracao_execucao_ms) as duracao_media_ms,
    SUM(CASE WHEN status = 'COMPLETADO' THEN 1 ELSE 0 END) as sucessos,
    SUM(CASE WHEN status = 'FALHADO' THEN 1 ELSE 0 END) as falhas
FROM jobs 
WHERE criado_em >= DATE_SUB(NOW(), INTERVAL 30 DAY)
GROUP BY DATE(criado_em), tipo, status
ORDER BY data DESC, tipo;

-- View para jobs ativos
CREATE OR REPLACE VIEW `v_jobs_ativos` AS
SELECT 
    id,
    nome,
    tipo,
    status,
    prioridade,
    grupo,
    tentativas,
    max_tentativas,
    circuit_breaker_aberto,
    agendado_para,
    proximo_retry,
    criado_em
FROM jobs 
WHERE status NOT IN ('COMPLETADO', 'CANCELADO', 'DESATIVADO', 'ARQUIVADO')
ORDER BY prioridade_peso DESC, criado_em ASC;

-- View para performance de jobs
CREATE OR REPLACE VIEW `v_job_performance` AS
SELECT 
    nome,
    tipo,
    COUNT(*) as total_execucoes,
    AVG(duracao_execucao_ms) as duracao_media_ms,
    MIN(duracao_execucao_ms) as duracao_min_ms,
    MAX(duracao_execucao_ms) as duracao_max_ms,
    SUM(CASE WHEN status = 'COMPLETADO' THEN 1 ELSE 0 END) as sucessos,
    SUM(CASE WHEN status IN ('FALHADO', 'TIMEOUT') THEN 1 ELSE 0 END) as falhas,
    (SUM(CASE WHEN status = 'COMPLETADO' THEN 1 ELSE 0 END) / COUNT(*)) * 100 as taxa_sucesso
FROM jobs 
WHERE total_execucoes > 0
GROUP BY nome, tipo
ORDER BY taxa_sucesso DESC, duracao_media_ms ASC;