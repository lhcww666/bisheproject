-- Python Agent Gateway：会话、消息、运行、工具审计、预约草稿与菜单权限
-- 使用前提：已执行 sql/module/00-module-menu-directory.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS agent_chat_session (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '所属用户',
  title VARCHAR(100) NOT NULL COMMENT '会话标题',
  engine VARCHAR(20) NOT NULL DEFAULT 'python' COMMENT 'AI 引擎',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '会话状态',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_agent_chat_session_user (user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Python Agent 会话';

CREATE TABLE IF NOT EXISTS agent_chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id BIGINT NOT NULL COMMENT '会话ID',
  role VARCHAR(20) NOT NULL COMMENT 'USER/ASSISTANT/SYSTEM',
  content MEDIUMTEXT NOT NULL COMMENT '消息内容',
  trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  reference_json MEDIUMTEXT DEFAULT NULL COMMENT '知识引用',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_agent_chat_message_session (session_id, id),
  CONSTRAINT fk_agent_chat_message_session FOREIGN KEY (session_id) REFERENCES agent_chat_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Python Agent 消息';

CREATE TABLE IF NOT EXISTS agent_run (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id BIGINT DEFAULT NULL COMMENT '会话ID',
  user_id BIGINT DEFAULT NULL COMMENT '用户ID',
  engine VARCHAR(20) NOT NULL COMMENT 'AI 引擎',
  intent VARCHAR(40) DEFAULT NULL COMMENT '识别意图',
  status VARCHAR(20) NOT NULL COMMENT '运行状态',
  trace_id VARCHAR(64) NOT NULL COMMENT '链路追踪ID',
  model_name VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  error_code VARCHAR(80) DEFAULT NULL COMMENT '错误码',
  error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  start_time DATETIME DEFAULT NULL,
  end_time DATETIME DEFAULT NULL,
  cost_ms BIGINT DEFAULT 0 COMMENT '耗时毫秒',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_agent_run_trace (trace_id),
  KEY idx_agent_run_session (session_id, id),
  KEY idx_agent_run_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 运行记录';

CREATE TABLE IF NOT EXISTS agent_tool_call_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id BIGINT DEFAULT NULL COMMENT '会话ID',
  message_id BIGINT DEFAULT NULL COMMENT '助手消息ID',
  user_id BIGINT DEFAULT NULL COMMENT '调用用户',
  tool_name VARCHAR(80) NOT NULL COMMENT '工具名称',
  tool_call_id VARCHAR(80) DEFAULT NULL COMMENT '工具调用ID',
  arguments MEDIUMTEXT DEFAULT NULL COMMENT '脱敏工具参数',
  result MEDIUMTEXT DEFAULT NULL COMMENT '脱敏工具结果',
  success CHAR(1) NOT NULL DEFAULT '1' COMMENT '1成功，0失败',
  cost_ms BIGINT DEFAULT 0 COMMENT '耗时毫秒',
  error_code VARCHAR(80) DEFAULT NULL COMMENT '错误码',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_agent_tool_call_session (session_id, id),
  KEY idx_agent_tool_call_message (message_id, id),
  KEY idx_agent_tool_call_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Python Agent 工具调用审计';

CREATE TABLE IF NOT EXISTS agent_reservation_draft (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID',
  session_id BIGINT DEFAULT NULL COMMENT '会话ID',
  request_json MEDIUMTEXT NOT NULL COMMENT '预约参数快照',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '草稿状态',
  expires_at DATETIME NOT NULL COMMENT '过期时间',
  confirmed_reservation_id BIGINT DEFAULT NULL COMMENT '确认后预约ID',
  idempotency_key VARCHAR(100) NOT NULL COMMENT '幂等键',
  create_by VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT NULL,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_agent_reservation_draft_idempotency (idempotency_key),
  KEY idx_agent_reservation_draft_user (user_id, status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Python Agent 预约草稿';

SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能助手', @moduleMenuId, 12, 'agent', 'module/agent/index', 1, 0, 'C', '1', '0',
       'module:agent:use', 'message', 'admin', sysdate(), 'Python Agent 与 Dify 智能助手'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:agent:use');

SET @agentMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:agent:use' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Agent 审计', @agentMenuId, 1, '', NULL, 1, 0, 'F', '0', '0',
       'module:agent:audit', '#', 'admin', sysdate(), '查看 Agent 运行与工具审计'
FROM (SELECT 1) t
WHERE @agentMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:agent:audit');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms = 'module:agent:use'
WHERE r.role_key IN ('student', 'teacher', 'admin');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms = 'module:agent:audit'
WHERE r.role_key = 'admin';

SELECT menu_name, path, component, menu_type, visible, perms
FROM sys_menu
WHERE perms LIKE 'module:agent:%';
