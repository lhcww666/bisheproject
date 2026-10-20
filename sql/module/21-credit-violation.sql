-- 信用账户、违规记录与信用分调整入口
-- 使用前提：已执行 sql/module_core.sql、sql/module/20-reservation-lifecycle.sql
-- 可重复执行：表、列、字典、菜单均先判重
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

DROP PROCEDURE IF EXISTS db_module_add_credit_biz_column;
DELIMITER $$
CREATE PROCEDURE db_module_add_credit_biz_column(IN col VARCHAR(64), IN definition VARCHAR(300))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'credit_record' AND COLUMN_NAME = col) THEN
    SET @ddl = CONCAT('ALTER TABLE credit_record ADD COLUMN `', col, '` ', definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL db_module_add_credit_biz_column('biz_type', 'VARCHAR(30) DEFAULT NULL COMMENT ''业务类型'' AFTER reason');
CALL db_module_add_credit_biz_column('biz_id', 'BIGINT DEFAULT NULL COMMENT ''业务主键'' AFTER biz_type');
DROP PROCEDURE IF EXISTS db_module_add_credit_biz_column;

CREATE TABLE IF NOT EXISTS credit_account (
  user_id BIGINT NOT NULL COMMENT '用户ID',
  score INT NOT NULL DEFAULT 100 COMMENT '当前信用分',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分账户';

CREATE TABLE IF NOT EXISTS violation_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '学生用户ID',
  reservation_id BIGINT DEFAULT NULL COMMENT '关联预约ID',
  violation_type VARCHAR(30) NOT NULL COMMENT '违规类型',
  description VARCHAR(500) DEFAULT NULL COMMENT '违规说明',
  credit_change INT NOT NULL DEFAULT 0 COMMENT '信用变动，负数表示扣分',
  status CHAR(1) NOT NULL DEFAULT '1' COMMENT '0待处理，1已处理，2已撤销',
  handler_id BIGINT DEFAULT NULL COMMENT '处理人ID',
  handle_time DATETIME DEFAULT NULL COMMENT '处理时间',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_violation_user_time (user_id, create_time),
  KEY idx_violation_reservation (reservation_id),
  CONSTRAINT fk_violation_user FOREIGN KEY (user_id) REFERENCES sys_user (user_id),
  CONSTRAINT fk_violation_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='违规记录';

-- 初始化信用账户：优先取最近一条流水的余额，没有流水按 100 分
INSERT INTO credit_account (user_id, score, update_time)
SELECT u.user_id,
       COALESCE((SELECT cr.balance_score FROM credit_record cr
                 WHERE cr.user_id = u.user_id ORDER BY cr.id DESC LIMIT 1), 100),
       sysdate()
FROM sys_user u
WHERE (EXISTS (SELECT 1 FROM sys_user_role ur JOIN sys_role r ON r.role_id = ur.role_id
               WHERE ur.user_id = u.user_id AND r.role_key = 'student')
       OR EXISTS (SELECT 1 FROM credit_record cr WHERE cr.user_id = u.user_id))
  AND NOT EXISTS (SELECT 1 FROM credit_account ca WHERE ca.user_id = u.user_id);

-- 字典
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '违规类型', 'module_violation_type', '0', 'admin', sysdate(), '爽约、临近取消、安全违规、设备损坏等'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_violation_type');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '违规记录状态', 'module_violation_status', '0', 'admin', sysdate(), '待处理、已处理、已撤销'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_violation_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '爽约', 'NO_SHOW', 'module_violation_type', '', 'danger', 'N', '0', 'admin', sysdate(), '预约后未签到'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_violation_type' AND d.dict_value = 'NO_SHOW');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '临近开始取消', 'LATE_CANCEL', 'module_violation_type', '', 'warning', 'N', '0', 'admin', sysdate(), '开始前 2 小时内取消'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_violation_type' AND d.dict_value = 'LATE_CANCEL');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '安全违规', 'SAFETY_VIOLATION', 'module_violation_type', '', 'danger', 'N', '0', 'admin', sysdate(), '违反实验室安全规定'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_violation_type' AND d.dict_value = 'SAFETY_VIOLATION');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '设备损坏', 'EQUIPMENT_DAMAGE', 'module_violation_type', '', 'danger', 'N', '0', 'admin', sysdate(), '损坏实验室设备'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_violation_type' AND d.dict_value = 'EQUIPMENT_DAMAGE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '其他', 'OTHER', 'module_violation_type', '', 'info', 'N', '0', 'admin', sysdate(), '其他违规'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_violation_type' AND d.dict_value = 'OTHER');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待处理', '0', 'module_violation_status', '', 'warning', 'N', '0', 'admin', sysdate(), '等待管理员处理'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_violation_status' AND d.dict_value = '0');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已处理', '1', 'module_violation_status', '', 'success', 'N', '0', 'admin', sysdate(), '已处理并扣分'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_violation_status' AND d.dict_value = '1');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已撤销', '2', 'module_violation_status', '', 'info', 'N', '0', 'admin', sysdate(), '已撤销并回补信用分'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_violation_status' AND d.dict_value = '2');

-- 菜单：违规记录
SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '违规记录', @moduleMenuId, 8, 'violationRecord', 'module/violationRecord/index', 1, 0, 'C', '0', '0',
       'module:violationRecord:list', 'warning', 'admin', sysdate(), '实验室违规与信用扣分记录'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:violationRecord:list');

SET @violationMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:violationRecord:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '违规记录查询', @violationMenuId, 1, '#', '', 1, 0, 'F', '0', '0',
       'module:violationRecord:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @violationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:violationRecord:query');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '违规登记', @violationMenuId, 2, '#', '', 1, 0, 'F', '0', '0',
       'module:violationRecord:add', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @violationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:violationRecord:add');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '撤销违规', @violationMenuId, 3, '#', '', 1, 0, 'F', '0', '0',
       'module:violationRecord:edit', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @violationMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:violationRecord:edit');

-- 信用分调整按钮
SET @creditMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:creditRecord:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '调整信用分', @creditMenuId, 10, '#', '', 1, 0, 'F', '0', '0',
       'module:creditRecord:adjust', '#', 'admin', sysdate(), '管理员手工调整学生信用分'
FROM (SELECT 1) t
WHERE @creditMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:creditRecord:adjust');

-- 角色授权
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN ('module:violationRecord:list', 'module:violationRecord:query')
WHERE r.role_key IN ('student', 'teacher');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN ('module:violationRecord:list', 'module:violationRecord:query',
                               'module:violationRecord:add', 'module:violationRecord:edit',
                               'module:creditRecord:adjust')
WHERE r.role_key = 'admin';

SELECT m.perms, m.menu_name, m.parent_id
FROM sys_menu m
WHERE m.perms LIKE 'module:violationRecord:%'
   OR m.perms = 'module:creditRecord:adjust'
ORDER BY m.perms;
