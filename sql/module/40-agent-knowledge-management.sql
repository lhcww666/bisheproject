-- Agent 安全知识库管理菜单与按钮权限
-- 使用前提：已执行 sql/module/24-agent.sql、sql/module/26-agent-rag.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @moduleMenuId := (
  SELECT menu_id FROM sys_menu
  WHERE menu_name = '实验室业务' AND menu_type = 'M'
  LIMIT 1
);

UPDATE sys_menu
SET parent_id = @moduleMenuId,
    path = 'agentKnowledge',
    order_num = 14
WHERE perms = 'module:agent:knowledge:list';

INSERT INTO sys_menu (
  menu_name, parent_id, order_num, path, component, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT 'Agent知识库', @moduleMenuId, 14, 'agentKnowledge', 'module/agent/knowledge/index',
       1, 0, 'C', '0', '0', 'module:agent:knowledge:list', 'documentation',
       'admin', sysdate(), '维护安全知识文档与知识片段'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM sys_menu) m
    WHERE m.perms = 'module:agent:knowledge:list'
  );

SET @knowledgeMenuId := (
  SELECT menu_id FROM sys_menu
  WHERE perms = 'module:agent:knowledge:list'
  LIMIT 1
);

INSERT INTO sys_menu (
  menu_name, parent_id, order_num, path, component, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT '知识库查询', @knowledgeMenuId, 1, '', '', 1, 0, 'F', '0', '0',
       'module:agent:knowledge:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @knowledgeMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM sys_menu) m
    WHERE m.perms = 'module:agent:knowledge:query'
  );

INSERT INTO sys_menu (
  menu_name, parent_id, order_num, path, component, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT '知识库新增', @knowledgeMenuId, 2, '', '', 1, 0, 'F', '0', '0',
       'module:agent:knowledge:add', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @knowledgeMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM sys_menu) m
    WHERE m.perms = 'module:agent:knowledge:add'
  );

INSERT INTO sys_menu (
  menu_name, parent_id, order_num, path, component, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT '知识库修改', @knowledgeMenuId, 3, '', '', 1, 0, 'F', '0', '0',
       'module:agent:knowledge:edit', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @knowledgeMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM sys_menu) m
    WHERE m.perms = 'module:agent:knowledge:edit'
  );

INSERT INTO sys_menu (
  menu_name, parent_id, order_num, path, component, is_frame, is_cache,
  menu_type, visible, status, perms, icon, create_by, create_time, remark
)
SELECT '知识库删除', @knowledgeMenuId, 4, '', '', 1, 0, 'F', '0', '0',
       'module:agent:knowledge:remove', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @knowledgeMenuId IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM sys_menu) m
    WHERE m.perms = 'module:agent:knowledge:remove'
  );

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms LIKE 'module:agent:knowledge:%'
WHERE r.role_key = 'admin';

SELECT menu_id, menu_name, parent_id, perms
FROM sys_menu
WHERE perms LIKE 'module:agent:knowledge:%'
ORDER BY parent_id, order_num, menu_id;
