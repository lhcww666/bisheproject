-- 收窄 hjl 开发测试角色：只保留实验室业务能力，移除系统管理和开发工具权限。
-- 使用前提：已执行模块菜单、实验室业务、准入、数据看板、智能助手等全部菜单脚本。
-- 可重复执行：角色按 role_key 定位，菜单授权先清后插。
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @testRoleId := (SELECT role_id FROM sys_role WHERE role_key = 'hjl' LIMIT 1);

UPDATE sys_role
SET role_name = '实验室测试管理员',
    data_scope = '1',
    status = '0',
    del_flag = '0',
    remark = '开发测试角色：仅允许测试实验室业务，不授予系统管理、代码生成和监控权限'
WHERE role_id = @testRoleId;

DELETE FROM sys_role_menu WHERE role_id = @testRoleId;

-- 先选中实验室业务按钮，再递归补齐所有上级目录，避免出现“有按钮权限但菜单不显示”。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
WITH RECURSIVE selected_menu AS (
  SELECT menu_id, parent_id
  FROM sys_menu
  WHERE perms IN (
    'module:stats:view',
    'module:room:list', 'module:room:query', 'module:room:add', 'module:room:edit',
    'module:room:remove', 'module:room:export', 'module:room:import',
    'module:device:list', 'module:device:query', 'module:device:add', 'module:device:edit',
    'module:device:remove', 'module:device:export', 'module:device:import',
    'module:reservation:list', 'module:reservation:query', 'module:reservation:add',
    'module:reservation:edit', 'module:reservation:remove', 'module:reservation:export',
    'module:reservation:import', 'module:reservation:review', 'module:reservation:available',
    'module:safetyExamRecord:list', 'module:safetyExamRecord:query',
    'module:safetyExamRecord:add', 'module:safetyExamRecord:edit',
    'module:safetyExamRecord:remove', 'module:safetyExamRecord:export',
    'module:safetyExamRecord:import',
    'module:creditRecord:list', 'module:creditRecord:query', 'module:creditRecord:adjust',
    'module:violationRecord:list', 'module:violationRecord:query',
    'module:violationRecord:add', 'module:violationRecord:edit',
    'module:roomAccess:list', 'module:roomAccess:query', 'module:roomAccess:add',
    'module:roomAccess:edit', 'module:roomAccess:remove',
    'module:classRepresentative:list', 'module:classRepresentative:query',
    'module:classRepresentative:add', 'module:classRepresentative:edit',
    'module:classRepresentative:remove',
    'module:safetyQuestion:list', 'module:safetyQuestion:query',
    'module:safetyQuestion:add', 'module:safetyQuestion:edit', 'module:safetyQuestion:remove',
    'module:agent:use'
  )
  UNION
  SELECT parent.menu_id, parent.parent_id
  FROM sys_menu parent
  JOIN selected_menu child ON child.parent_id = parent.menu_id
  WHERE parent.menu_id <> 0
)
SELECT @testRoleId, menu_id
FROM selected_menu
WHERE menu_id <> 0;

SELECT r.role_id, r.role_name, r.role_key, r.data_scope,
       COUNT(rm.menu_id) AS menu_count,
       SUM(CASE WHEN m.menu_type = 'C' THEN 1 ELSE 0 END) AS page_count,
       SUM(CASE WHEN m.perms LIKE 'system:%' THEN 1 ELSE 0 END) AS system_permission_count
FROM sys_role r
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
LEFT JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE r.role_key = 'hjl'
GROUP BY r.role_id, r.role_name, r.role_key, r.data_scope;
