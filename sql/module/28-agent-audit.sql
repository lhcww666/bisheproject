-- Agent 会话与工具调用审计菜单
-- 仅管理员可见；会话、消息和工具调用日志由 Agent 运行过程自动写入。
-- 使用前提：已执行 sql/module/24-agent.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Agent审计', @moduleMenuId, 13, 'agentAudit', 'module/agentAudit/index', 1, 0, 'C', '0', '0',
       'module:agent:audit', 'monitor', 'admin', sysdate(), '查看智能助手会话、消息和工具调用审计'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:agent:audit');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms = 'module:agent:audit'
WHERE r.role_key = 'admin';

SELECT menu_name, path, component, perms
FROM sys_menu
WHERE perms = 'module:agent:audit';
