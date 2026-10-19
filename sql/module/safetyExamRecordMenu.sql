-- 菜单 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('安全考试记录', '3', '1', 'safetyExamRecord', 'module/safetyExamRecord/list', 1, 0, 'C', '0', '0', 'module:safetyExamRecord:list', '#', 'admin', sysdate(), '', null, '安全考试记录菜单')
    ;
    -- 按钮父菜单ID
    SELECT @parentId := LAST_INSERT_ID();

-- 按钮 SQL
insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('安全考试记录查询', @parentId, '1',  '#', '', 1, 0, 'F', '0', '0', 'module:safetyExamRecord:query',        '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('安全考试记录新增', @parentId, '2',  '#', '', 1, 0, 'F', '0', '0', 'module:safetyExamRecord:add',          '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('安全考试记录修改', @parentId, '3',  '#', '', 1, 0, 'F', '0', '0', 'module:safetyExamRecord:edit',         '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('安全考试记录删除', @parentId, '4',  '#', '', 1, 0, 'F', '0', '0', 'module:safetyExamRecord:remove',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('安全考试记录导出', @parentId, '5',  '#', '', 1, 0, 'F', '0', '0', 'module:safetyExamRecord:export',       '#', 'admin', sysdate(), '', null, '');

insert into sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values('安全考试记录导入', @parentId, '6',  '#', '', 1, 0, 'F', '0', '0', 'module:safetyExamRecord:import',       '#', 'admin', sysdate(), '', null, '');

