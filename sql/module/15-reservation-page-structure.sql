-- 预约模块页面结构：列表使用 list.vue，详情/新增/修改使用独立路由页面
USE bishe_schema;

UPDATE sys_menu
SET component = 'module/reservation/list'
WHERE perms = 'module:reservation:list';

SELECT menu_id, menu_name, path, component, perms
FROM sys_menu
WHERE perms = 'module:reservation:list';
