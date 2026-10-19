-- 实验室准入规则（第一版）
-- 口径：实验室归属院系 + 预约模式 + 院系/班级/学生白名单。
-- 课程授权暂不启用，subject_type 预留 COURSE，后续增加课程表后再接入。
-- 使用前提：已执行 sql/module_core.sql、sql/module/08-role-menu-binding.sql。
-- 可重复执行：列、表、字典、菜单均先判重。
USE bishe_schema;

-- 1. room 增加归属院系和预约模式
DROP PROCEDURE IF EXISTS db_module_add_room_access_column;
DELIMITER $$
CREATE PROCEDURE db_module_add_room_access_column(IN tbl VARCHAR(64), IN col VARCHAR(64), IN definition VARCHAR(300))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
    SET @ddl = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ', definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL db_module_add_room_access_column('room', 'dept_id',
  'BIGINT NULL COMMENT ''归属院系ID（sys_dept.dept_id）'' AFTER location');
CALL db_module_add_room_access_column('room', 'reservation_mode',
  'VARCHAR(20) NOT NULL DEFAULT ''OPEN'' COMMENT ''预约模式：OPEN/RESTRICTED/APPROVAL_ONLY/CLOSED'' AFTER status');

DROP PROCEDURE IF EXISTS db_module_add_room_access_column;

-- 2. 准入规则表
CREATE TABLE IF NOT EXISTS room_access (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  room_id BIGINT NOT NULL COMMENT '实验室ID',
  subject_type VARCHAR(20) NOT NULL COMMENT '授权对象：DEPT/CLASS/USER，COURSE 预留',
  subject_id BIGINT NOT NULL COMMENT '部门ID/班级ID/学生用户ID',
  valid_from DATETIME DEFAULT NULL COMMENT '生效时间，空表示立即生效',
  valid_until DATETIME DEFAULT NULL COMMENT '失效时间，空表示长期有效',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_access_subject (room_id, subject_type, subject_id),
  KEY idx_room_access_subject (subject_type, subject_id),
  CONSTRAINT fk_room_access_room FOREIGN KEY (room_id) REFERENCES room (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室准入规则表';

-- 3. 字典
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '实验室预约模式', 'module_reservation_mode', '0', 'admin', sysdate(), '实验室是否开放学生预约及是否需要审核'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_reservation_mode');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '实验室准入对象', 'module_room_access_subject', '0', 'admin', sysdate(), '院系、班级或具体学生'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_room_access_subject');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '开放预约', 'OPEN', 'module_reservation_mode', '', 'primary', 'Y', '0', 'admin', sysdate(), '符合信用分、考试等条件的学生均可预约'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_reservation_mode' AND d.dict_value = 'OPEN');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '限定范围', 'RESTRICTED', 'module_reservation_mode', '', 'warning', 'N', '0', 'admin', sysdate(), '只有已配置准入规则的学生可以预约'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_reservation_mode' AND d.dict_value = 'RESTRICTED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '限定范围且需审核', 'APPROVAL_ONLY', 'module_reservation_mode', '', 'danger', 'N', '0', 'admin', sysdate(), '有准入规则的学生提交后必须教师审核'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_reservation_mode' AND d.dict_value = 'APPROVAL_ONLY');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '停止预约', 'CLOSED', 'module_reservation_mode', '', 'info', 'N', '0', 'admin', sysdate(), '学生端不可预约，管理员仍可维护'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_reservation_mode' AND d.dict_value = 'CLOSED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '院系', 'DEPT', 'module_room_access_subject', '', 'primary', 'Y', '0', 'admin', sysdate(), '授权该院系及其下级班级'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_room_access_subject' AND d.dict_value = 'DEPT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '班级', 'CLASS', 'module_room_access_subject', '', 'success', 'N', '0', 'admin', sysdate(), '只授权指定班级'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_room_access_subject' AND d.dict_value = 'CLASS');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '学生', 'USER', 'module_room_access_subject', '', 'info', 'N', '0', 'admin', sysdate(), '只授权指定学生，适合毕设、竞赛等例外场景'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_room_access_subject' AND d.dict_value = 'USER');

-- 4. 菜单
SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '实验室准入', @moduleMenuId, 6, 'roomAccess', 'module/roomAccess/list', 1, 0, 'C', '0', '0', 'module:roomAccess:list', 'lock', 'admin', sysdate(), '实验室准入规则'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:roomAccess:list');

SET @roomAccessMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:roomAccess:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '准入查询', @roomAccessMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'module:roomAccess:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @roomAccessMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:roomAccess:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '准入新增', @roomAccessMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'module:roomAccess:add', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @roomAccessMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:roomAccess:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '准入修改', @roomAccessMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'module:roomAccess:edit', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @roomAccessMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:roomAccess:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '准入删除', @roomAccessMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'module:roomAccess:remove', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @roomAccessMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:roomAccess:remove');

-- 管理型角色默认可见（超级管理员本身不受菜单限制）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN ('module:roomAccess:list', 'module:roomAccess:query', 'module:roomAccess:add', 'module:roomAccess:edit', 'module:roomAccess:remove')
WHERE r.role_key IN ('admin', 'hjl');

-- 5. 历史实验室补归属院系，并给两条实验室配置演示准入规则
SET @rootDeptId := (SELECT dept_id FROM sys_dept WHERE parent_id = 0 ORDER BY dept_id LIMIT 1);

UPDATE room SET dept_id = @rootDeptId WHERE dept_id IS NULL;
UPDATE room SET reservation_mode = 'OPEN' WHERE reservation_mode IS NULL OR reservation_mode = '';
UPDATE room SET reservation_mode = 'RESTRICTED' WHERE room_code = 'A101' AND reservation_mode = 'OPEN';
UPDATE room SET reservation_mode = 'APPROVAL_ONLY' WHERE room_code = 'C301' AND reservation_mode = 'OPEN';

INSERT INTO room_access (room_id, subject_type, subject_id, create_by, create_time, remark)
SELECT r.id, 'DEPT', @rootDeptId, 'admin', sysdate(), '演示：院系内学生可预约'
FROM room r
WHERE @rootDeptId IS NOT NULL
  AND r.room_code IN ('A101', 'C301')
  AND NOT EXISTS (
    SELECT 1 FROM (SELECT * FROM room_access) ra
    WHERE ra.room_id = r.id AND ra.subject_type = 'DEPT' AND ra.subject_id = @rootDeptId
  );

SELECT r.id, r.room_code, r.reservation_mode, r.dept_id, COUNT(ra.id) AS access_count
FROM room r LEFT JOIN room_access ra ON ra.room_id = r.id
GROUP BY r.id, r.room_code, r.reservation_mode, r.dept_id
ORDER BY r.id;
