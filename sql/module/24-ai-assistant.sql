-- Spring AI 智能助手：会话、消息、工具调用审计与菜单权限
-- 使用前提：已执行 sql/module/00-module-menu-directory.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS chat_session (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '所属用户',
  title VARCHAR(100) NOT NULL COMMENT '会话标题',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_chat_session_user (user_id, update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能助手会话';

CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id BIGINT NOT NULL COMMENT '会话ID',
  role VARCHAR(20) NOT NULL COMMENT 'USER/ASSISTANT/SYSTEM',
  content TEXT NOT NULL COMMENT '消息内容',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_chat_message_session (session_id, id),
  CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_session (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能助手消息';

CREATE TABLE IF NOT EXISTS tool_call_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id BIGINT DEFAULT NULL COMMENT '会话ID',
  user_id BIGINT DEFAULT NULL COMMENT '调用用户',
  tool_name VARCHAR(80) NOT NULL COMMENT '工具名称',
  arguments TEXT COMMENT '工具参数',
  result TEXT COMMENT '工具返回',
  success CHAR(1) NOT NULL DEFAULT '1' COMMENT '1成功，0失败',
  cost_ms INT DEFAULT 0 COMMENT '耗时毫秒',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tool_call_session (session_id, id),
  KEY idx_tool_call_user (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='智能助手工具调用审计';

SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '智能助手', @moduleMenuId, 12, 'assistant', 'module/assistant/index', 1, 0, 'C', '0', '0',
       'module:assistant:use', 'message', 'admin', sysdate(), '受控的实验室智能助手'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:assistant:use');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms = 'module:assistant:use'
WHERE r.role_key IN ('student', 'teacher', 'admin');

SELECT menu_name, path, component, perms
FROM sys_menu
WHERE perms = 'module:assistant:use';
