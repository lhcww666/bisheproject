-- 安全考试题库、试卷配置、在线答题与菜单权限
-- 使用前提：已执行 sql/module_core.sql、sql/module/12-demo-class-data.sql
-- 可重复执行：列、表、题目、字典、菜单均先判重
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

DROP PROCEDURE IF EXISTS db_module_add_exam_record_column;
DELIMITER $$
CREATE PROCEDURE db_module_add_exam_record_column(IN col VARCHAR(64), IN definition VARCHAR(300))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'safety_exam_record' AND COLUMN_NAME = col) THEN
    SET @ddl = CONCAT('ALTER TABLE safety_exam_record ADD COLUMN `', col, '` ', definition);
    PREPARE stmt FROM @ddl;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL db_module_add_exam_record_column('config_id', 'BIGINT DEFAULT NULL COMMENT ''试卷配置ID'' AFTER valid_until');
CALL db_module_add_exam_record_column('paper_no', 'VARCHAR(32) DEFAULT NULL COMMENT ''试卷编号'' AFTER config_id');
CALL db_module_add_exam_record_column('submit_time', 'DATETIME DEFAULT NULL COMMENT ''交卷时间'' AFTER paper_no');
CALL db_module_add_exam_record_column('status', 'CHAR(1) NOT NULL DEFAULT ''1'' COMMENT ''0进行中，1已提交，2已过期'' AFTER submit_time');
CALL db_module_add_exam_record_column('correct_count', 'INT DEFAULT 0 COMMENT ''答对题数'' AFTER status');
DROP PROCEDURE IF EXISTS db_module_add_exam_record_column;

CREATE TABLE IF NOT EXISTS safety_exam_config (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  config_name VARCHAR(100) NOT NULL COMMENT '配置名称',
  question_count INT NOT NULL DEFAULT 10 COMMENT '抽题数量',
  pass_score INT NOT NULL DEFAULT 80 COMMENT '通过分数',
  duration_minutes INT NOT NULL DEFAULT 30 COMMENT '考试时长（分钟）',
  valid_days INT NOT NULL DEFAULT 365 COMMENT '通过后有效期（天）',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0启用，1停用',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全考试配置';

CREATE TABLE IF NOT EXISTS safety_question (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  question_code VARCHAR(30) NOT NULL COMMENT '题目编号',
  question_type VARCHAR(20) NOT NULL COMMENT 'SINGLE/MULTIPLE/JUDGE',
  content VARCHAR(1000) NOT NULL COMMENT '题干',
  options_json JSON NOT NULL COMMENT '选项 JSON 数组',
  answer_json JSON NOT NULL COMMENT '答案 JSON 数组，值为 A/B/C/D',
  score INT NOT NULL DEFAULT 10 COMMENT '分值',
  safety_level TINYINT NOT NULL DEFAULT 1 COMMENT '安全等级',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0启用，1停用',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_safety_question_code (question_code),
  KEY idx_safety_question_status (status, safety_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全考试题库';

CREATE TABLE IF NOT EXISTS safety_exam_answer (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  record_id BIGINT NOT NULL COMMENT '考试记录ID',
  question_id BIGINT NOT NULL COMMENT '题目ID',
  answer_json JSON DEFAULT NULL COMMENT '学生答案 JSON 数组',
  is_correct CHAR(1) DEFAULT NULL COMMENT '1正确，0错误，NULL未判分',
  score INT DEFAULT NULL COMMENT '本题得分',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_exam_answer_question (record_id, question_id),
  KEY idx_exam_answer_record (record_id),
  CONSTRAINT fk_exam_answer_record FOREIGN KEY (record_id) REFERENCES safety_exam_record (id),
  CONSTRAINT fk_exam_answer_question FOREIGN KEY (question_id) REFERENCES safety_question (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全考试答题明细';

INSERT INTO safety_exam_config (config_name, question_count, pass_score, duration_minutes, valid_days, status, create_by, create_time, remark)
SELECT '实验室安全准入考试', 10, 80, 30, 365, '0', 'admin', sysdate(), '默认配置：随机10题，80分通过，有效期一年'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM safety_exam_config WHERE config_name = '实验室安全准入考试');

INSERT IGNORE INTO safety_question
  (question_code, question_type, content, options_json, answer_json, score, safety_level, status, create_by, create_time, remark)
VALUES
('Q001', 'SINGLE', '化学品溅入眼睛时，首先应该怎么做？',
 JSON_ARRAY('A. 用大量清水冲洗至少15分钟', 'B. 用手揉眼睛', 'C. 用纸巾擦拭', 'D. 等待老师处理'), JSON_ARRAY('A'), 10, 1, '0', 'seed', sysdate(), '眼部应急处理'),
('Q002', 'SINGLE', '酒精灯不慎打翻起火，最合适的处理方式是？',
 JSON_ARRAY('A. 用水浇灭', 'B. 用湿抹布或灭火毯覆盖', 'C. 用嘴吹灭', 'D. 直接跑开'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '易燃液体火灾'),
('Q003', 'SINGLE', '进入化学实验室必须穿戴的防护用品是？',
 JSON_ARRAY('A. 拖鞋和短裤', 'B. 实验服和护目镜', 'C. 只戴手套', 'D. 不需要'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '个人防护'),
('Q004', 'SINGLE', '稀释浓硫酸的正确操作是？',
 JSON_ARRAY('A. 将水快速倒入浓硫酸', 'B. 将浓硫酸沿器壁缓慢倒入水中并搅拌', 'C. 在密闭容器中混合', 'D. 用手搅拌'), JSON_ARRAY('B'), 10, 2, '0', 'seed', sysdate(), '浓硫酸稀释'),
('Q005', 'SINGLE', '闻试剂气味时应？',
 JSON_ARRAY('A. 把鼻子凑到瓶口', 'B. 用手轻轻扇动闻', 'C. 深呼吸', 'D. 直接品尝'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '闻气味方法'),
('Q006', 'SINGLE', '电器着火时首先应？',
 JSON_ARRAY('A. 用水扑救', 'B. 切断电源后用干粉灭火器', 'C. 用手拔电源', 'D. 打开门窗'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '电气火灾'),
('Q007', 'SINGLE', '使用离心机前必须？',
 JSON_ARRAY('A. 随意放置离心管', 'B. 对称配平并盖好盖子', 'C. 超速运行', 'D. 打开盖子观察'), JSON_ARRAY('B'), 10, 2, '0', 'seed', sysdate(), '离心机操作'),
('Q008', 'SINGLE', '实验废液应如何处理？',
 JSON_ARRAY('A. 倒入下水道', 'B. 分类收集到指定废液桶', 'C. 倒回原试剂瓶', 'D. 放在实验台上'), JSON_ARRAY('B'), 10, 2, '0', 'seed', sysdate(), '废液处置'),
('Q009', 'SINGLE', '加热后的玻璃仪器应？',
 JSON_ARRAY('A. 立即用手拿', 'B. 用坩埚钳或隔热手套取用', 'C. 立即用冷水冲', 'D. 直接放回试剂柜'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '高温防护'),
('Q010', 'SINGLE', '实验室发生火灾时，正确做法是？',
 JSON_ARRAY('A. 乘电梯离开', 'B. 保持冷静，按疏散路线撤离并报警', 'C. 躲进实验柜', 'D. 继续实验'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '应急疏散'),
('Q011', 'SINGLE', '使用易燃溶剂时，应该？',
 JSON_ARRAY('A. 靠近明火', 'B. 在通风橱内操作并远离火源', 'C. 紧闭门窗', 'D. 边抽烟边操作'), JSON_ARRAY('B'), 10, 2, '0', 'seed', sysdate(), '易燃溶剂'),
('Q012', 'SINGLE', '用移液管吸取液体时，应该？',
 JSON_ARRAY('A. 用嘴吸', 'B. 使用洗耳球', 'C. 直接倾倒', 'D. 用手堵住管口'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '移液操作'),
('Q013', 'SINGLE', '实验室里可以饮食吗？',
 JSON_ARRAY('A. 可以', 'B. 不可以', 'C. 只要老师同意就可以', 'D. 无人时可以'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '实验室纪律'),
('Q014', 'SINGLE', '使用高压灭菌锅前应？',
 JSON_ARRAY('A. 不检查直接使用', 'B. 检查水位、密封并排尽冷空气', 'C. 超压运行', 'D. 打开盖子运行'), JSON_ARRAY('B'), 10, 3, '0', 'seed', sysdate(), '高压设备'),
('Q015', 'SINGLE', '发现有人触电时，首先应？',
 JSON_ARRAY('A. 直接用手拉人', 'B. 先切断电源或用绝缘物分离', 'C. 向触电者泼水', 'D. 离开现场'), JSON_ARRAY('B'), 10, 2, '0', 'seed', sysdate(), '触电急救'),
('Q016', 'SINGLE', '发现试剂标签脱落时，应？',
 JSON_ARRAY('A. 继续使用', 'B. 停止使用并报告管理员', 'C. 自己随便贴标签', 'D. 闻气味判断'), JSON_ARRAY('B'), 10, 2, '0', 'seed', sysdate(), '试剂管理'),
('Q017', 'SINGLE', '实验结束离开实验室前应？',
 JSON_ARRAY('A. 直接离开', 'B. 关闭水电气、整理台面并洗手', 'C. 让设备继续运行', 'D. 不锁门'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '离室检查'),
('Q018', 'SINGLE', '危险废物容器应？',
 JSON_ARRAY('A. 敞口放置', 'B. 有清晰标签并密闭', 'C. 与生活垃圾混放', 'D. 随意摆放'), JSON_ARRAY('B'), 10, 2, '0', 'seed', sysdate(), '危废管理'),
('Q019', 'MULTIPLE', '以下哪些属于实验室禁止行为？',
 JSON_ARRAY('A. 饮食', 'B. 吸烟', 'C. 穿实验服', 'D. 独自进行高危实验'), JSON_ARRAY('A', 'B', 'D'), 10, 2, '0', 'seed', sysdate(), '多选题'),
('Q020', 'MULTIPLE', '火灾逃生时，正确的做法包括？',
 JSON_ARRAY('A. 低姿撤离', 'B. 乘电梯', 'C. 用湿毛巾捂住口鼻', 'D. 返回取个人物品'), JSON_ARRAY('A', 'C'), 10, 1, '0', 'seed', sysdate(), '多选题'),
('Q021', 'JUDGE', '判断题：实验服可以穿出实验室进入食堂。',
 JSON_ARRAY('正确', '错误'), JSON_ARRAY('B'), 10, 1, '0', 'seed', sysdate(), '判断题'),
('Q022', 'JUDGE', '判断题：少量实验废液可以直接倒入下水道。',
 JSON_ARRAY('正确', '错误'), JSON_ARRAY('B'), 10, 2, '0', 'seed', sysdate(), '判断题');

SET @idx_exists := (SELECT COUNT(1) FROM information_schema.STATISTICS
                    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'safety_exam_record'
                      AND INDEX_NAME = 'idx_safety_record_student_status');
SET @sql := IF(@idx_exists = 0,
  'ALTER TABLE safety_exam_record ADD INDEX idx_safety_record_student_status (student_id, status)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '安全考试题型', 'module_exam_question_type', '0', 'admin', sysdate(), '单选、多选、判断'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_exam_question_type');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '安全考试记录状态', 'module_exam_record_status', '0', 'admin', sysdate(), '进行中、已提交、已过期'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_exam_record_status');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '单选题', 'SINGLE', 'module_exam_question_type', '', 'primary', 'Y', '0', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_exam_question_type' AND d.dict_value = 'SINGLE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '多选题', 'MULTIPLE', 'module_exam_question_type', '', 'success', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_exam_question_type' AND d.dict_value = 'MULTIPLE');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '判断题', 'JUDGE', 'module_exam_question_type', '', 'warning', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_exam_question_type' AND d.dict_value = 'JUDGE');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '进行中', '0', 'module_exam_record_status', '', 'warning', 'Y', '0', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_exam_record_status' AND d.dict_value = '0');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已提交', '1', 'module_exam_record_status', '', 'success', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_exam_record_status' AND d.dict_value = '1');
INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已过期', '2', 'module_exam_record_status', '', 'info', 'N', '0', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d WHERE d.dict_type = 'module_exam_record_status' AND d.dict_value = '2');

-- 菜单：学生安全考试
SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '安全考试', @moduleMenuId, 9, 'safetyExam', 'module/safetyExam/index', 1, 0, 'C', '0', '0',
       'module:safetyExam:status', 'education', 'admin', sysdate(), '学生安全考试与在线答题'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:safetyExam:status');

SET @examMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:safetyExam:status' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '开始考试', @examMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'module:safetyExam:start', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE @examMenuId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:safetyExam:start');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '提交试卷', @examMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'module:safetyExam:submit', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE @examMenuId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:safetyExam:submit');

-- 菜单：管理员题库
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库管理', @moduleMenuId, 10, 'safetyQuestion', 'module/safetyQuestion/index', 1, 0, 'C', '0', '0',
       'module:safetyQuestion:list', 'question', 'admin', sysdate(), '安全考试题库维护'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:safetyQuestion:list');

SET @questionMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:safetyQuestion:list' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库查询', @questionMenuId, 1, '#', '', 1, 0, 'F', '0', '0', 'module:safetyQuestion:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE @questionMenuId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:safetyQuestion:query');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库新增', @questionMenuId, 2, '#', '', 1, 0, 'F', '0', '0', 'module:safetyQuestion:add', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE @questionMenuId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:safetyQuestion:add');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库修改', @questionMenuId, 3, '#', '', 1, 0, 'F', '0', '0', 'module:safetyQuestion:edit', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE @questionMenuId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:safetyQuestion:edit');
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '题库删除', @questionMenuId, 4, '#', '', 1, 0, 'F', '0', '0', 'module:safetyQuestion:remove', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t WHERE @questionMenuId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:safetyQuestion:remove');

-- 角色授权
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r JOIN sys_menu m
  ON m.perms IN ('module:safetyExam:status', 'module:safetyExam:start', 'module:safetyExam:submit')
WHERE r.role_key = 'student';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r JOIN sys_menu m
  ON m.perms IN ('module:safetyExam:status', 'module:safetyExam:start', 'module:safetyExam:submit',
                 'module:safetyQuestion:list', 'module:safetyQuestion:query', 'module:safetyQuestion:add',
                 'module:safetyQuestion:edit', 'module:safetyQuestion:remove')
WHERE r.role_key = 'admin';
