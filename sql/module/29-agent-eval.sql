-- Agent 离线评测：30 条典型自然语言用例与评测结果
-- 当前评测模式为 OFFLINE：确定性意图路由 + RAG 检索命中，不需要模型 Key。
-- 使用前提：已执行 sql/module/26-agent-rag.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS agent_eval_case (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  case_code VARCHAR(30) NOT NULL COMMENT '用例编号',
  category VARCHAR(30) NOT NULL COMMENT '用例分类',
  question VARCHAR(500) NOT NULL COMMENT '自然语言问题',
  expected_intent VARCHAR(50) NOT NULL COMMENT '期望意图',
  expected_tool VARCHAR(80) NOT NULL COMMENT '期望工具',
  expected_keywords VARCHAR(500) DEFAULT NULL COMMENT 'RAG期望关键词，逗号分隔',
  enabled CHAR(1) NOT NULL DEFAULT '1' COMMENT '1启用，0停用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_agent_eval_case_code (case_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent离线评测用例';

CREATE TABLE IF NOT EXISTS agent_eval_run (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  run_no VARCHAR(40) NOT NULL COMMENT '评测批次号',
  mode VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT 'OFFLINE/LLM',
  total INT NOT NULL DEFAULT 0 COMMENT '用例总数',
  passed INT NOT NULL DEFAULT 0 COMMENT '通过数',
  intent_accuracy DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '意图准确率',
  tool_accuracy DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '工具路由准确率',
  rag_hit_rate DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT 'RAG命中率',
  pass_rate DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '综合通过率',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_agent_eval_run_no (run_no),
  KEY idx_agent_eval_run_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent评测批次';

CREATE TABLE IF NOT EXISTS agent_eval_result (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  run_id BIGINT NOT NULL COMMENT '评测批次',
  case_id BIGINT NOT NULL COMMENT '用例',
  category VARCHAR(30) NOT NULL,
  question VARCHAR(500) NOT NULL,
  expected_intent VARCHAR(50) NOT NULL,
  expected_tool VARCHAR(80) NOT NULL,
  actual_intent VARCHAR(50) DEFAULT NULL,
  actual_tool VARCHAR(80) DEFAULT NULL,
  passed CHAR(1) NOT NULL DEFAULT '0',
  tool_success CHAR(1) NOT NULL DEFAULT '0',
  actual_answer TEXT,
  duration_ms INT DEFAULT 0,
  error_message VARCHAR(500) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_agent_eval_result_run (run_id, id),
  CONSTRAINT fk_agent_eval_result_run FOREIGN KEY (run_id) REFERENCES agent_eval_run (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent评测明细';

INSERT IGNORE INTO agent_eval_case (case_code, category, question, expected_intent, expected_tool, expected_keywords) VALUES
('E001', 'EXAM', '我通过安全考试了吗？', 'EXAM_STATUS', 'checkSafetyExamStatus', NULL),
('E002', 'EXAM', '我的安全考试有效期到什么时候？', 'EXAM_STATUS', 'checkSafetyExamStatus', NULL),
('E003', 'EXAM', '我考试过了吗？', 'EXAM_STATUS', 'checkSafetyExamStatus', NULL),
('E004', 'CREDIT', '我的信用分是多少？', 'CREDIT', 'queryMyCredit', NULL),
('E005', 'CREDIT', '信用分低于60还能预约吗？', 'CREDIT', 'queryMyCredit', NULL),
('E006', 'CREDIT', '我的预约资格够吗？', 'CREDIT', 'queryMyCredit', NULL),
('E007', 'MY_RESERVATION', '查看我最近的预约', 'MY_RESERVATIONS', 'queryMyReservations', NULL),
('E008', 'MY_RESERVATION', '我有哪些已完成的预约？', 'MY_RESERVATIONS', 'queryMyReservations', NULL),
('E009', 'MY_RESERVATION', '我的预约记录在哪里看？', 'MY_RESERVATIONS', 'queryMyReservations', NULL),
('E010', 'REVIEW', '我的预约为什么被驳回？', 'REVIEW', 'queryReservationReview', NULL),
('E011', 'REVIEW', '审核意见在哪里看？', 'REVIEW', 'queryReservationReview', NULL),
('E012', 'REVIEW', '这个预约为什么没通过？', 'REVIEW', 'queryReservationReview', NULL),
('E013', 'ROOMS', '下周三下午有哪些实验室可以使用？', 'QUERY_ROOMS', 'queryAvailableRooms', NULL),
('E014', 'ROOMS', '哪里可以做有机实验？', 'QUERY_ROOMS', 'queryAvailableRooms', NULL),
('E015', 'ROOMS', '明天上午空闲的实验室有哪些？', 'QUERY_ROOMS', 'queryAvailableRooms', NULL),
('E016', 'ROOMS', '有没有能容纳十个人的实验室？', 'QUERY_ROOMS', 'queryAvailableRooms', NULL),
('E017', 'DEVICES', '生物实验室有哪些设备？', 'DEVICES', 'queryAvailableDevices', NULL),
('E018', 'DEVICES', '离心机现在可以用吗？', 'DEVICES', 'queryAvailableDevices', NULL),
('E019', 'SAFETY', '酒精灯打翻着火怎么办？', 'SAFETY', 'searchSafetyKnowledge', '酒精,湿抹布'),
('E020', 'SAFETY', '浓硫酸稀释应该怎么做？', 'SAFETY', 'searchSafetyKnowledge', '浓硫酸,水,搅拌'),
('E021', 'SAFETY', '化学品溅到眼睛里怎么处理？', 'SAFETY', 'searchSafetyKnowledge', '清水,冲洗'),
('E022', 'SAFETY', '实验室废液可以倒下水道吗？', 'SAFETY', 'searchSafetyKnowledge', '废液,分类'),
('E023', 'SAFETY', '电器着火怎么灭火？', 'SAFETY', 'searchSafetyKnowledge', '切断电源,干粉'),
('E024', 'SAFETY', '离心机操作安全注意事项', 'SAFETY', 'searchSafetyKnowledge', '配平,盖子'),
('E025', 'SAFETY', '进入实验室要穿什么？', 'SAFETY', 'searchSafetyKnowledge', '实验服,护目镜'),
('E026', 'SAFETY', '发生火灾怎么疏散？', 'SAFETY', 'searchSafetyKnowledge', '疏散,电梯'),
('E027', 'SAFETY', '高压灭菌锅怎么用？', 'SAFETY', 'searchSafetyKnowledge', '水位,密封'),
('E028', 'SAFETY', '酒精着火能用水浇吗？', 'SAFETY', 'searchSafetyKnowledge', '酒精,水'),
('E029', 'DRAFT', '帮我预约下周三下午两点到四点，两个人做有机实验', 'CREATE_DRAFT', 'createReservationDraft', NULL),
('E030', 'DRAFT', '我想预约实验室做材料实验，先帮我生成草稿', 'CREATE_DRAFT', 'createReservationDraft', NULL);

SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'Agent评测', @moduleMenuId, 14, 'agentEval', 'module/agentEval/index', 1, 0, 'C', '0', '0',
       'module:agent:eval', 'chart', 'admin', sysdate(), 'Agent离线评测与指标报告'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:agent:eval');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms = 'module:agent:eval'
WHERE r.role_key = 'admin';
