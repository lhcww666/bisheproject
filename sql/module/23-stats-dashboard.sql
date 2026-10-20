-- 角色感知的数据看板菜单与权限
-- 使用前提：已执行 sql/module/00-module-menu-directory.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据看板', @moduleMenuId, 11, 'dashboard', 'module/dashboard/index', 1, 0, 'C', '0', '0',
       'module:stats:view', 'chart', 'admin', sysdate(), '预约、使用率、违规和考试概览'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:stats:view');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms = 'module:stats:view'
WHERE r.role_key IN ('student', 'teacher', 'admin');

SELECT menu_name, path, component, perms
FROM sys_menu
WHERE perms = 'module:stats:view';
