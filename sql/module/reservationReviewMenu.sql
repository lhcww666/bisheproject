-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('预约审核', '3', '1', 'reservationReview', 'module/reservationReview/list', 1, 0, 'C', '0', '0', 'module:reservationReview:list', '#', 'admin', sysdate(), '', null, '预约审核菜单')
    ;
    -- 按钮父菜单ID
    SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('预约审核查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'module:reservationReview:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('预约审核导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'module:reservationReview:export',       '#', 'admin', sysdate(), '', null, '');

