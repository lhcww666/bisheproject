-- 学生与教师角色及其菜单授权
-- 使用前提：已执行 00-module-menu-directory.sql、06/07 两个按钮权限脚本
-- 本脚本可重复执行：角色按 role_key 判重，授权先清后插
-- 数据范围口径：sys_user.dept_id 表示学生所在班级；教师分配到班级部门后，
--               只能查看本部门（同一班级）中的学生记录。
USE bishe_schema;

-- 1. 角色
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '学生', 'student', 3, '5', 1, 1, '0', '0', 'admin', sysdate(), '学生角色：实验室资源、预约申请、安全考试与信用分'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_role) r WHERE r.role_key = 'student');

INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT '教师', 'teacher', 4, '3', 1, 1, '0', '0', 'admin', sysdate(), '教师角色：预约审核、本班学生资质与资源查看'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_role) r WHERE r.role_key = 'teacher');

SET @studentRoleId := (SELECT role_id FROM sys_role WHERE role_key = 'student' LIMIT 1);
SET @teacherRoleId := (SELECT role_id FROM sys_role WHERE role_key = 'teacher' LIMIT 1);
SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

-- 已有角色也同步数据范围，避免历史库仍保留“全部数据”设置
UPDATE sys_role SET data_scope = '5', remark = '学生角色：预约申请、本人信用分与安全考试'
WHERE role_key = 'student';
UPDATE sys_role SET data_scope = '3', remark = '教师角色：预约审核、本班学生资质与资源查看'
WHERE role_key = 'teacher';

-- 2. 学生菜单授权：预约申请/我的预约/取消 + 本人安全考试与信用分
--    不授予实验室档案、设备档案菜单；预约页通过专用查询接口选择可用资源
DELETE FROM sys_role_menu WHERE role_id = @studentRoleId;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @studentRoleId, @moduleMenuId
FROM (SELECT 1) t WHERE @moduleMenuId IS NOT NULL;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @studentRoleId, m.menu_id FROM sys_menu m
WHERE m.perms IN (
 'module:reservation:list', 'module:reservation:query', 'module:reservation:apply',
 'module:reservation:my', 'module:reservation:cancel', 'module:reservation:available',
 'module:safetyExamRecord:list', 'module:safetyExamRecord:query',
 'module:creditRecord:list', 'module:creditRecord:query',
 'module:violationRecord:list', 'module:violationRecord:query'
);

-- 3. 教师菜单授权：审核 + 相关资源与资质查看
DELETE FROM sys_role_menu WHERE role_id = @teacherRoleId;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @teacherRoleId, @moduleMenuId
FROM (SELECT 1) t WHERE @moduleMenuId IS NOT NULL;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @teacherRoleId, m.menu_id FROM sys_menu m
WHERE m.perms IN (
 'module:room:list', 'module:room:query',
 'module:device:list', 'module:device:query',
 'module:reservation:list', 'module:reservation:query', 'module:reservation:review',
 'module:safetyExamRecord:list', 'module:safetyExamRecord:query',
 'module:creditRecord:list', 'module:creditRecord:query',
 'module:violationRecord:list', 'module:violationRecord:query'
);

SELECT r.role_id, r.role_name, r.role_key, COUNT(rm.menu_id) AS menu_count
FROM sys_role r LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id
WHERE r.role_key IN ('student', 'teacher')
GROUP BY r.role_id, r.role_name, r.role_key;
