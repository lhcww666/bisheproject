-- 数据看板作为首页内容，隐藏“实验室业务”下的重复看板菜单。
-- 使用前提：已执行 sql/module/23-stats-dashboard.sql。
-- 可重复执行。
USE bishe_schema;

UPDATE sys_menu
SET visible = '1'
WHERE perms = 'module:stats:view'
  AND menu_type = 'C';

SELECT menu_name, path, component, visible
FROM sys_menu
WHERE perms = 'module:stats:view';
