-- 将工具调用审计记录绑定到对应的助手消息，刷新会话后仍可展示调用明细
-- 使用前提：已执行 sql/module/24-agent.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

DROP PROCEDURE IF EXISTS db_agent_add_tool_message_column;
DELIMITER $$
CREATE PROCEDURE db_agent_add_tool_message_column()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'tool_call_log'
                   AND COLUMN_NAME = 'message_id') THEN
    ALTER TABLE tool_call_log
      ADD COLUMN message_id BIGINT DEFAULT NULL COMMENT '助手消息ID' AFTER session_id;
  END IF;
END$$
DELIMITER ;

CALL db_agent_add_tool_message_column();
DROP PROCEDURE db_agent_add_tool_message_column;

DROP PROCEDURE IF EXISTS db_agent_add_tool_message_index;
DELIMITER $$
CREATE PROCEDURE db_agent_add_tool_message_index()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'tool_call_log'
                   AND INDEX_NAME = 'idx_tool_call_message') THEN
    ALTER TABLE tool_call_log ADD INDEX idx_tool_call_message (message_id, id);
  END IF;
END$$
DELIMITER ;

CALL db_agent_add_tool_message_index();
DROP PROCEDURE db_agent_add_tool_message_index;

-- 旧版本只按会话记录工具调用，这里按“同会话中调用之后的最近一条助手消息”回填一次。
UPDATE tool_call_log log
JOIN (
  SELECT candidates.id, MIN(messages.id) AS message_id
  FROM tool_call_log candidates
  JOIN chat_message messages
    ON messages.session_id = candidates.session_id
   AND messages.role = 'ASSISTANT'
   AND messages.create_time >= candidates.create_time
  WHERE candidates.session_id IS NOT NULL
    AND candidates.message_id IS NULL
  GROUP BY candidates.id
) linked ON linked.id = log.id
SET log.message_id = linked.message_id;

SELECT id, session_id, message_id, tool_name, success, create_time
FROM tool_call_log
ORDER BY id DESC
LIMIT 10;
