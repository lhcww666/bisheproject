-- 为缺少审计字段的业务表补齐 create_by / create_time / update_by / update_time / remark
-- 背景：代码生成器产出的实体统一继承 BaseEntity，MyBatis-Plus 的查询会把 BaseEntity 的字段一并拼进 SQL。
--       room、device、reservation 三张表本来就有这些列，但 reservation_device、reservation_review、
--       safety_exam_record、credit_record 没有，导致这 4 个页面一打开就报
--       "Unknown column 'create_by' in 'field list'"，列表查不出数据。
-- 做法：与《数据库设计说明》不冲突的前提下，给这 4 张表补上审计列，保持与其余 3 张表一致，页面无需改代码。
-- 使用前提：已执行 sql/module_core.sql（或 sql/module/ 下更早的脚本）。
-- 可重复执行：脚本内部先查 information_schema，只在列不存在时才 ALTER。
USE bishe_schema;

DROP PROCEDURE IF EXISTS db_module_add_audit_column;
DELIMITER $$
CREATE PROCEDURE db_module_add_audit_column(IN tbl VARCHAR(64), IN col VARCHAR(64), IN definition VARCHAR(200))
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

-- 预约设备关联表
CALL db_module_add_audit_column('reservation_device', 'create_by',   'VARCHAR(64) DEFAULT '''' COMMENT ''创建者''');
CALL db_module_add_audit_column('reservation_device', 'create_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''');
CALL db_module_add_audit_column('reservation_device', 'update_by',   'VARCHAR(64) DEFAULT '''' COMMENT ''更新者''');
CALL db_module_add_audit_column('reservation_device', 'update_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL db_module_add_audit_column('reservation_device', 'remark',      'VARCHAR(500) DEFAULT NULL COMMENT ''备注''');

-- 预约审核表
CALL db_module_add_audit_column('reservation_review', 'create_by',   'VARCHAR(64) DEFAULT '''' COMMENT ''创建者''');
CALL db_module_add_audit_column('reservation_review', 'create_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''');
CALL db_module_add_audit_column('reservation_review', 'update_by',   'VARCHAR(64) DEFAULT '''' COMMENT ''更新者''');
CALL db_module_add_audit_column('reservation_review', 'update_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL db_module_add_audit_column('reservation_review', 'remark',      'VARCHAR(500) DEFAULT NULL COMMENT ''备注''');

-- 安全考试记录表
CALL db_module_add_audit_column('safety_exam_record', 'create_by',   'VARCHAR(64) DEFAULT '''' COMMENT ''创建者''');
CALL db_module_add_audit_column('safety_exam_record', 'create_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''');
CALL db_module_add_audit_column('safety_exam_record', 'update_by',   'VARCHAR(64) DEFAULT '''' COMMENT ''更新者''');
CALL db_module_add_audit_column('safety_exam_record', 'update_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL db_module_add_audit_column('safety_exam_record', 'remark',      'VARCHAR(500) DEFAULT NULL COMMENT ''备注''');

-- 信用分流水表（create_time 原本就有，这里会被跳过）
CALL db_module_add_audit_column('credit_record', 'create_by',   'VARCHAR(64) DEFAULT '''' COMMENT ''创建者''');
CALL db_module_add_audit_column('credit_record', 'create_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间''');
CALL db_module_add_audit_column('credit_record', 'update_by',   'VARCHAR(64) DEFAULT '''' COMMENT ''更新者''');
CALL db_module_add_audit_column('credit_record', 'update_time', 'DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间''');
CALL db_module_add_audit_column('credit_record', 'remark',      'VARCHAR(500) DEFAULT NULL COMMENT ''备注''');

DROP PROCEDURE IF EXISTS db_module_add_audit_column;

-- 自检：下面每张表都应能查到 create_by、create_time、update_by、update_time、remark 五行
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('reservation_device', 'reservation_review', 'safety_exam_record', 'credit_record')
  AND COLUMN_NAME IN ('create_by', 'create_time', 'update_by', 'update_time', 'remark')
ORDER BY TABLE_NAME, COLUMN_NAME;
