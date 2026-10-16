-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('实验室信息', '3', '1', 'room', 'module/room/index', 1, 0, 'C', '0', '0', 'module:room:list', '#', 'admin', sysdate(), '', null, '实验室信息菜单')
    ;
    -- 按钮父菜单ID
    SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('实验室信息查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'module:room:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('实验室信息新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'module:room:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('实验室信息修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'module:room:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('实验室信息删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'module:room:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('实验室信息导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'module:room:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('实验室信息导入', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'module:room:import',       '#', 'admin', sysdate(), '', null, '');

