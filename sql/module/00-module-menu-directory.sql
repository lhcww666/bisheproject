-- 实验室业务目录菜单：把代码生成器产出的模块菜单，统一挂到独立的一级目录下
-- 使用前提：已执行 sql/module 下各模块的 *Menu.sql
USE bishe_schema;

-- 1. 创建一级目录菜单（若不存在）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实验室业务', 0, 5, 'module', NULL, 1, 0, 'M', '0', '0', '', 'education', 'admin', sysdate(), '实验室业务模块目录'
FROM (SELECT 1) tmp
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) t WHERE t.menu_name = '实验室业务' AND t.menu_type = 'M');

-- 2. 把各模块列表菜单挂到该目录下
SET @parentMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

UPDATE sys_menu SET parent_id = @parentMenuId
WHERE menu_type = 'C' AND (perms LIKE 'module:%:list' OR perms LIKE 'module:%:my');
