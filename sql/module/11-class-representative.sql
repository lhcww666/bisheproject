-- 班级负责人与班级预约（方案 C）
-- 口径：班级负责人可以代表本班提交班级预约；普通学生仍可提交个人预约。
-- 班级预约默认需要教师审核，参与人数按班级学生数计算。
-- 使用前提：已执行 sql/module_core.sql、sql/module/10-room-access.sql。
-- 可重复执行：列、表、字典、菜单均先判重。
USE bishe_schema;

-- 1. reservation 增加预约类型和班级
DROP PROCEDURE IF EXISTS db_module_add_class_reservation_column;
DELIMITER $$
CREATE PROCEDURE db_module_add_class_reservation_column(IN tbl VARCHAR(64), IN col VARCHAR(64), IN definition VARCHAR(300))
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

CALL db_module_add_class_reservation_column('reservation', 'reservation_type',
  'VARCHAR(20) NOT NULL DEFAULT ''INDIVIDUAL'' COMMENT ''预约类型：INDIVIDUAL/CLASS'' AFTER participant_count');
CALL db_module_add_class_reservation_column('reservation', 'class_dept_id',
  'BIGINT NULL COMMENT ''班级预约对应班级ID（sys_dept.dept_id）'' AFTER reservation_type');

DROP PROCEDURE IF EXISTS db_module_add_class_reservation_column;

-- 2. 班级负责人表：一个班一个负责人，一个学生只能担任一个班的负责人
CREATE TABLE IF NOT EXISTS class_representative (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  dept_id BIGINT NOT NULL COMMENT '班级ID（sys_dept.dept_id）',
  student_id BIGINT NOT NULL COMMENT '学生ID（sys_user.user_id）',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态：0正常，1停用',
  valid_from DATETIME DEFAULT NULL COMMENT '生效时间，空表示立即生效',
  valid_until DATETIME DEFAULT NULL COMMENT '失效时间，空表示长期有效',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_class_representative_dept (dept_id),
  UNIQUE KEY uk_class_representative_student (student_id),
  KEY idx_class_representative_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级负责人表';

-- 3. 字典
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '预约类型', 'module_reservation_type', '0', 'admin', sysdate(), '个人预约或班级预约'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_reservation_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '个人预约', 'INDIVIDUAL', 'module_reservation_type', '', 'primary', 'Y', '0', 'admin', sysdate(), '学生本人提交的预约'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_reservation_type' AND d.dict_value = 'INDIVIDUAL');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '班级预约', 'CLASS', 'module_reservation_type', '', 'success', 'N', '0', 'admin', sysdate(), '班级负责人代表本班提交的预约'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_reservation_type' AND d.dict_value = 'CLASS');

-- 4. 菜单
SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '班级负责人', @moduleMenuId, 7, 'classRepresentative', 'module/classRepresentative/list', 1, 0, 'C', '0', '0', 'module:classRepresentative:list', 'peoples', 'admin', sysdate(), '班级负责人与班级预约资格'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:classRepresentative:list');

SET @classRepresentativeMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:classRepresentative:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '负责人查询', @classRepresentativeMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'module:classRepresentative:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @classRepresentativeMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:classRepresentative:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '负责人新增', @classRepresentativeMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'module:classRepresentative:add', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @classRepresentativeMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:classRepresentative:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '负责人修改', @classRepresentativeMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'module:classRepresentative:edit', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @classRepresentativeMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:classRepresentative:edit');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '负责人删除', @classRepresentativeMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'module:classRepresentative:remove', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @classRepresentativeMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:classRepresentative:remove');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN ('module:classRepresentative:list', 'module:classRepresentative:query', 'module:classRepresentative:add', 'module:classRepresentative:edit', 'module:classRepresentative:remove')
WHERE r.role_key IN ('admin', 'hjl');

-- 5. 演示数据：student01 作为“总公司”的班级负责人
INSERT INTO class_representative (dept_id, student_id, status, create_by, create_time, remark)
SELECT 100, 101, '0', 'admin', sysdate(), '演示：student01 作为总公司班级负责人'
FROM (SELECT 1) t
WHERE EXISTS (SELECT 1 FROM sys_user u WHERE u.user_id = 101 AND u.dept_id = 100)
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM class_representative) r WHERE r.dept_id = 100)
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM class_representative) r WHERE r.student_id = 101);

SELECT r.id, r.dept_id, d.dept_name, r.student_id, u.nick_name, r.status
FROM class_representative r
LEFT JOIN sys_dept d ON d.dept_id = r.dept_id
LEFT JOIN sys_user u ON u.user_id = r.student_id
ORDER BY r.id;
