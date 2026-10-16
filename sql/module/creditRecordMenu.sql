-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('信用分流水', '3', '1', 'creditRecord', 'module/creditRecord/index', 1, 0, 'C', '0', '0', 'module:creditRecord:list', '#', 'admin', sysdate(), '', null, '信用分流水菜单')
    ;
    -- 按钮父菜单ID
    SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('信用分流水查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'module:creditRecord:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('信用分流水新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'module:creditRecord:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('信用分流水修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'module:creditRecord:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('信用分流水删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'module:creditRecord:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('信用分流水导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'module:creditRecord:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('信用分流水导入', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'module:creditRecord:import',       '#', 'admin', sysdate(), '', null, '');

