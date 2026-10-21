-- 将已存在的智能助手菜单从 assistant 命名迁移到 agent 命名
-- 使用前提：已执行 sql/module/24-ai-assistant.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

UPDATE sys_menu
SET menu_name = '智能助手',
    path = 'agent',
    component = 'module/agent/index',
    perms = 'module:agent:use',
    remark = '受控的实验室智能助手'
WHERE perms = 'module:assistant:use'
   OR path = 'assistant'
   OR component = 'module/assistant/index';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms = 'module:agent:use'
WHERE r.role_key IN ('student', 'teacher', 'admin');

SELECT menu_name, path, component, perms
FROM sys_menu
WHERE perms = 'module:agent:use';
