-- 预约生命周期：签到、完成、爽约、取消原因与状态日志
-- 使用前提：已执行 sql/module_core.sql、sql/module/13-reservation-audit.sql
-- 可重复执行：列、表、索引、字典均先判重
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

DROP PROCEDURE IF EXISTS db_module_add_reservation_lifecycle_column;
DELIMITER $$
CREATE PROCEDURE db_module_add_reservation_lifecycle_column(IN col VARCHAR(64), IN definition VARCHAR(300))
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

CALL db_module_add_reservation_lifecycle_column('check_in_time',
  'DATETIME DEFAULT NULL COMMENT ''签到时间'' AFTER check_date');
CALL db_module_add_reservation_lifecycle_column('finish_time',
  'DATETIME DEFAULT NULL COMMENT ''实验完成时间'' AFTER check_in_time');
CALL db_module_add_reservation_lifecycle_column('actual_participant_count',
  'INT DEFAULT NULL COMMENT ''实际参与人数'' AFTER finish_time');
CALL db_module_add_reservation_lifecycle_column('cancel_time',
  'DATETIME DEFAULT NULL COMMENT ''取消时间'' AFTER actual_participant_count');
CALL db_module_add_reservation_lifecycle_column('cancel_reason',
  'VARCHAR(500) DEFAULT NULL COMMENT ''取消原因'' AFTER cancel_time');

DROP PROCEDURE IF EXISTS db_module_add_reservation_lifecycle_column;

CREATE TABLE IF NOT EXISTS reservation_status_log (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  reservation_id BIGINT NOT NULL COMMENT '预约ID',
  from_status CHAR(1) DEFAULT NULL COMMENT '变更前状态',
  to_status CHAR(1) NOT NULL COMMENT '变更后状态',
  action VARCHAR(30) NOT NULL COMMENT '动作：CREATE/APPROVE/REJECT/UNAUDIT/CANCEL/CHECK_IN/FINISH/NO_SHOW/EDIT',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人ID',
  operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
  reason VARCHAR(500) DEFAULT NULL COMMENT '原因或说明',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (id),
  KEY idx_status_log_reservation (reservation_id, create_time),
  KEY idx_status_log_action (action, create_time),
  CONSTRAINT fk_status_log_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约状态变更日志';

-- 允许同一预约多次审核：弃审后重新审核需要追加新的审核记录
DROP PROCEDURE IF EXISTS db_module_relax_review_unique;
DELIMITER $$
CREATE PROCEDURE db_module_relax_review_unique()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation_review'
                   AND INDEX_NAME = 'idx_review_reservation') THEN
    ALTER TABLE reservation_review ADD INDEX idx_review_reservation (reservation_id, id);
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation_review'
               AND INDEX_NAME = 'uk_review_reservation') THEN
    ALTER TABLE reservation_review DROP INDEX uk_review_reservation;
  END IF;
END$$
DELIMITER ;
CALL db_module_relax_review_unique();
DROP PROCEDURE IF EXISTS db_module_relax_review_unique;

-- 预约生命周期状态字典
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '已签到', '5', 'module_reservation_status', '', 'primary', 'N', '0', 'admin', sysdate(), '学生已签到'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_reservation_status' AND d.dict_value = '5');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 6, '已爽约', '6', 'module_reservation_status', '', 'danger', 'N', '0', 'admin', sysdate(), '超过签到时限未签到'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_reservation_status' AND d.dict_value = '6');

-- 常用查询索引
SET @idx_exists := (SELECT COUNT(1) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'reservation'
                      AND INDEX_NAME = 'idx_reservation_status_time');
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE reservation ADD INDEX idx_reservation_status_time (status, start_time)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT id, reservation_no, status, check_in_time, finish_time, cancel_time
FROM reservation
ORDER BY id DESC
LIMIT 10;
