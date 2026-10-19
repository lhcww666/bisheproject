-- 菜单瘦身：把中间表维护页移到“数据维护”，不再占用实验室业务主菜单
-- 涉及：预约设备关联（reservation_device）、预约审核记录（reservation_review）
USE bishe_schema;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '数据维护', 0, 99, 'dataMaintenance', NULL, 1, 0, 'M', '0', '0', '', 'tool', 'admin', sysdate(), '管理员数据维护入口'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.menu_name = '数据维护' AND m.menu_type = 'M');

SET @dataMaintenanceMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '数据维护' AND menu_type = 'M' LIMIT 1);

UPDATE sys_menu
SET parent_id = @dataMaintenanceMenuId,
    menu_name = '预约设备明细',
    order_num = 1
WHERE perms = 'module:reservationDevice:list';

UPDATE sys_menu
SET parent_id = @dataMaintenanceMenuId,
    menu_name = '审核记录',
    order_num = 2
WHERE perms = 'module:reservationReview:list';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, @dataMaintenanceMenuId
FROM sys_role r
WHERE r.role_key IN ('admin', 'hjl');

SELECT menu_id, parent_id, menu_name, path, perms
FROM sys_menu
WHERE menu_id = @dataMaintenanceMenuId
   OR perms IN ('module:reservationDevice:list', 'module:reservationReview:list')
ORDER BY parent_id, order_num, menu_id;
