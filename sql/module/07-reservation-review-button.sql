-- 预约审核按钮权限：教师端待审核列表与审核操作
-- 使用前提：已执行 sql/module/reservationMenu.sql（存在权限标识 module:reservation:list 的菜单）
-- 本脚本可重复执行
USE bishe_schema;

SET @reservationMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:reservation:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '预约审核', @reservationMenuId, 11, '#', '', 1, 0, 'F', '0', '0', 'module:reservation:review', '#', 'admin', sysdate(), '教师审核通过或驳回预约'
FROM (SELECT 1) t
WHERE @reservationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:reservation:review');

SELECT m.menu_id, m.menu_name, m.parent_id, m.menu_type, m.perms
FROM sys_menu m
WHERE m.perms = 'module:reservation:review';
