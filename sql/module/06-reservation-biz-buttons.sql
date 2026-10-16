-- 预约业务按钮权限：提交申请、我的预约、取消预约、可用实验室查询
-- 使用前提：已执行 sql/module/reservationMenu.sql（存在权限标识 module:reservation:list 的菜单）
-- 本脚本可重复执行
USE bishe_schema;

SET @reservationMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:reservation:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '预约申请', @reservationMenuId, 7, '#', '', 1, 0, 'F', '0', '0', 'module:reservation:apply', '#', 'admin', sysdate(), '学生提交预约申请'
FROM (SELECT 1) t
WHERE @reservationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:reservation:apply');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '我的预约', @reservationMenuId, 8, '#', '', 1, 0, 'F', '0', '0', 'module:reservation:my', '#', 'admin', sysdate(), '查询本人预约'
FROM (SELECT 1) t
WHERE @reservationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:reservation:my');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '取消预约', @reservationMenuId, 9, '#', '', 1, 0, 'F', '0', '0', 'module:reservation:cancel', '#', 'admin', sysdate(), '取消本人预约'
FROM (SELECT 1) t
WHERE @reservationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:reservation:cancel');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '可用实验室查询', @reservationMenuId, 10, '#', '', 1, 0, 'F', '0', '0', 'module:reservation:available', '#', 'admin', sysdate(), '按时间段查询可用实验室'
FROM (SELECT 1) t
WHERE @reservationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:reservation:available');

SELECT m.menu_id, m.menu_name, m.parent_id, m.menu_type, m.perms
FROM sys_menu m
WHERE m.perms LIKE 'module:reservation:%'
ORDER BY m.menu_id;
