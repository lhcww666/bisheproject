-- 预约审核/弃审状态（对齐 aoliya 的 check_status 口径）
-- check_status: no_check 未审核，had_checked 已审核
-- 新增的预约一律先进入待审核；审核通过后才算业务生效。
-- 弃审不删除预约主单，只删除本次审核记录并回退到待审核。
-- 使用前提：已执行 sql/module_core.sql、sql/module/10-room-access.sql、sql/module/11-class-representative.sql。
-- 可重复执行。
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

DROP PROCEDURE IF EXISTS db_module_add_reservation_audit_column;
DELIMITER $$
CREATE PROCEDURE db_module_add_reservation_audit_column(IN col VARCHAR(64), IN definition VARCHAR(300))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation' AND COLUMN_NAME = col) THEN
    SET @ddl = CONCAT('ALTER TABLE reservation ADD COLUMN `', col, '` ', definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL db_module_add_reservation_audit_column('check_status',
  'VARCHAR(20) NOT NULL DEFAULT ''no_check'' COMMENT ''审核状态：no_check/had_checked'' AFTER status');
CALL db_module_add_reservation_audit_column('check_name',
  'VARCHAR(64) DEFAULT NULL COMMENT ''审核人'' AFTER check_status');
CALL db_module_add_reservation_audit_column('check_date',
  'DATETIME DEFAULT NULL COMMENT ''审核时间'' AFTER check_name');

DROP PROCEDURE IF EXISTS db_module_add_reservation_audit_column;

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '预约审核状态', 'module_check_status', '0', 'admin', sysdate(), '预约单是否已经审核'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_check_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '未审核', 'no_check', 'module_check_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '新增后等待审核'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_check_status' AND d.dict_value = 'no_check');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已审核', 'had_checked', 'module_check_status', '', 'success', 'N', '0', 'admin', sysdate(), '已经完成审核（通过或驳回）'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_check_status' AND d.dict_value = 'had_checked');

-- 历史数据回填：已通过/已驳回视为已审核，其余为未审核
UPDATE reservation r
LEFT JOIN reservation_review rr ON rr.reservation_id = r.id
LEFT JOIN sys_user u ON u.user_id = rr.reviewer_id
SET r.check_status = CASE WHEN r.status IN ('1', '2') THEN 'had_checked' ELSE 'no_check' END,
    r.check_name = CASE WHEN r.status IN ('1', '2') THEN COALESCE(NULLIF(u.nick_name, ''), u.user_name) ELSE NULL END,
    r.check_date = CASE WHEN r.status IN ('1', '2') THEN rr.review_time ELSE NULL END;

SELECT id, reservation_no, status, check_status, check_name, check_date
FROM reservation
ORDER BY id DESC
LIMIT 10;
