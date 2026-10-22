-- 统一实验室业务菜单图标，保证学生/教师展开后的菜单对齐一致。
-- 使用前提：已执行 sql/module/00-module-menu-directory.sql。
-- 可重复执行。
USE bishe_schema;

UPDATE sys_menu SET icon = 'money' WHERE perms = 'module:creditRecord:list';
UPDATE sys_menu SET icon = 'component' WHERE perms = 'module:device:list';
UPDATE sys_menu SET icon = 'date' WHERE perms = 'module:reservation:list';
UPDATE sys_menu SET icon = 'table' WHERE perms = 'module:room:list';
UPDATE sys_menu SET icon = 'documentation' WHERE perms = 'module:safetyExamRecord:list';
UPDATE sys_menu SET icon = 'bug' WHERE perms = 'module:violationRecord:list';
UPDATE sys_menu SET icon = 'lock' WHERE perms = 'module:roomAccess:list';
UPDATE sys_menu SET icon = 'peoples' WHERE perms = 'module:classRepresentative:list';
UPDATE sys_menu SET icon = 'education' WHERE perms = 'module:safetyExam:status';
UPDATE sys_menu SET icon = 'question' WHERE perms = 'module:safetyQuestion:list';
UPDATE sys_menu SET icon = 'message' WHERE perms = 'module:agent:use';
UPDATE sys_menu SET icon = 'monitor' WHERE perms = 'module:agent:audit';
UPDATE sys_menu SET icon = 'chart' WHERE perms = 'module:agent:eval';
UPDATE sys_menu SET icon = 'component' WHERE perms = 'module:reservationDevice:list';
UPDATE sys_menu SET icon = 'clipboard' WHERE perms = 'module:reservationReview:list';

SELECT menu_name, path, icon
FROM sys_menu
WHERE perms LIKE 'module:%' AND menu_type = 'C'
ORDER BY parent_id, order_num, menu_id;
