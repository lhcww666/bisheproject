-- Agent 平台帮助知识与角色可见范围
-- 使用前提：已执行 sql/module/26-agent-rag.sql、sql/module/40-agent-knowledge-management.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

DROP PROCEDURE IF EXISTS db_agent_add_knowledge_scope;
DELIMITER $$
CREATE PROCEDURE db_agent_add_knowledge_scope()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'knowledge_document'
                   AND COLUMN_NAME = 'knowledge_type') THEN
    ALTER TABLE knowledge_document
      ADD COLUMN knowledge_type VARCHAR(30) NOT NULL DEFAULT 'SAFETY'
        COMMENT 'SAFETY安全知识，PLATFORM平台帮助' AFTER category;
  END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'knowledge_document'
                   AND COLUMN_NAME = 'audience') THEN
    ALTER TABLE knowledge_document
      ADD COLUMN audience VARCHAR(30) NOT NULL DEFAULT 'ALL'
        COMMENT 'ALL/STUDENT/TEACHER/ADMIN' AFTER knowledge_type;
  END IF;
END$$
DELIMITER ;

CALL db_agent_add_knowledge_scope();
DROP PROCEDURE db_agent_add_knowledge_scope;

DROP PROCEDURE IF EXISTS db_agent_add_knowledge_scope_index;
DELIMITER $$
CREATE PROCEDURE db_agent_add_knowledge_scope_index()
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'knowledge_document'
                   AND INDEX_NAME = 'idx_knowledge_document_scope') THEN
    ALTER TABLE knowledge_document
      ADD INDEX idx_knowledge_document_scope (knowledge_type, audience, status);
  END IF;
END$$
DELIMITER ;

CALL db_agent_add_knowledge_scope_index();
DROP PROCEDURE db_agent_add_knowledge_scope_index;

UPDATE knowledge_document
SET knowledge_type = COALESCE(NULLIF(knowledge_type, ''), 'SAFETY'),
    audience = COALESCE(NULLIF(audience, ''), 'ALL');

INSERT IGNORE INTO knowledge_document
  (id, title, category, knowledge_type, audience, source, status, create_by, create_time, remark)
VALUES
  (2001, '智能助手能力与使用边界', '助手使用', 'PLATFORM', 'ALL', '平台使用说明', '0', 'admin', sysdate(),
   '说明助手可查询的业务、写操作确认方式和越界处理'),
  (2002, '设备档案使用说明', '设备管理', 'PLATFORM', 'ALL', '平台使用说明', '0', 'admin', sysdate(),
   '说明设备档案面向学生和教师展示的业务信息'),
  (2003, '实验室预约办理流程', '预约管理', 'PLATFORM', 'ALL', '平台使用说明', '0', 'admin', sysdate(),
   '从查询资格、选择时段到确认预约的完整流程'),
  (2004, '安全考试与信用分说明', '准入管理', 'PLATFORM', 'ALL', '平台使用说明', '0', 'admin', sysdate(),
   '说明安全考试、信用分与预约资格的关系'),
  (2005, '教师审核工作台说明', '审核管理', 'PLATFORM', 'TEACHER', '平台使用说明', '0', 'admin', sysdate(),
   '说明教师查看和确认预约审核的操作入口'),
  (2006, 'Agent知识库管理员说明', '助手使用', 'PLATFORM', 'ADMIN', '平台使用说明', '0', 'admin', sysdate(),
   '说明知识库文档、片段、可见范围和审计用途'),
  (2007, '设备档案数据字段说明', '数据维护', 'PLATFORM', 'ADMIN', '平台使用说明', '0', 'admin', sysdate(),
   '说明设备档案主要业务字段及其维护责任'),
  (2008, '实验室档案使用说明', '实验室管理', 'PLATFORM', 'ALL', '平台使用说明', '0', 'admin', sysdate(),
   '说明实验室编号、容量、开放时间和准入规则');

INSERT IGNORE INTO knowledge_chunk
  (id, document_id, chunk_no, keywords, content)
VALUES
  (20001, 2001, 1,
   '智能助手 Agent 能做什么 知识库 能力 边界 预约 设备 安全考试 信用分 审计',
   '智能助手可以查询当前用户有权查看的实验室时段、设备信息、预约记录、安全考试和信用分，也能检索安全知识与平台使用说明。学生可以在条件齐全后生成预约草稿，再在预约页面确认提交；教师可以整理审核依据，但最终审核需要在审核页面确认。助手不能代替用户提交预约、完成审核或查看无权数据。'),
  (20002, 2002, 1,
   '设备档案 是什么 存什么 数据 设备编号 设备名称 类型 实验室 数量 培训 状态',
   '设备档案用于记录实验室设备的基础业务信息，通常包括设备编号、设备名称、设备类型、所属实验室、可用数量、是否需要培训以及当前状态。学生和教师可以据此判断设备属于哪个实验室、是否需要培训、当前是否可以预约；内部技术字段和维护记录不在普通用户的回答范围内。'),
  (20003, 2003, 1,
   '预约流程 怎么预约 选实验室 预约草稿 确认提交 待审核 取消 签到',
   '办理预约时先确认安全考试、信用分和实验室准入条件，再选择实验室、日期、使用时段、参与人数和需要的设备。条件齐全后可以生成预约草稿，核对信息并点击“确认提交预约”。需要审核的预约会进入待审核状态，学生可在预约记录中查看进度和审核意见。'),
  (20004, 2004, 1,
   '安全考试 有效期 信用分 门槛 预约资格 准入',
   '预约资格由信用分、安全考试、实验室准入和实验室安全等级共同决定。信用分低于当前门槛时不能提交预约；要求安全考试的实验室还需要有效通过的考试记录。低风险实验室是否要求考试以实验室规则为准。'),
  (20005, 2005, 1,
   '教师 审核 待审核 审核依据 学生资质 审核页面 确认',
   '教师审核助手可以查看本人负责范围内的待审核预约、预约详情、学生安全考试和信用情况，并整理审核依据。最终通过、驳回或弃审必须在教师审核页面确认，助手不会代为提交审核结论。'),
  (20006, 2006, 1,
   'Agent知识库 是什么 文档 片段 可见范围 安全知识 平台帮助 审计',
   'Agent知识库由知识文档和可检索片段组成，包含实验室安全知识和平台使用说明。每篇文档可以设置安全知识或平台帮助类型，以及全部角色、学生、教师或管理员可见范围。助手只使用当前登录角色可见且已启用的片段，检索和工具调用会留下审计记录。'),
  (20007, 2007, 1,
   '设备档案 字段 数据维护 device_code device_name device_type room_id quantity requires_training status',
   '管理员维护设备档案时，主要字段包括设备编号、设备名称、设备类型、所属实验室、数量、是否需要培训、状态和备注。设备编号应保持唯一；停用或故障设备需要及时更新状态。删除前应确认没有被有效预约引用。'),
  (20008, 2008, 1,
   '实验室档案 实验室编号 名称 容量 开放时间 准入 安全等级 状态 怎么使用',
   '实验室档案用于查看实验室编号、名称、位置、容量、开放时间、预约模式、安全等级和当前状态。预约前应确认开放时间、容量、安全考试要求和准入范围；不符合准入规则时，即使时段空闲也不能提交预约。');

INSERT IGNORE INTO agent_eval_case
  (case_code, category, question, expected_intent, expected_tool, expected_keywords, enabled)
VALUES
  ('E051', 'PLATFORM_HELP', '这个 Agent 知识库是什么？', 'PLATFORM_HELP', 'searchPlatformHelp', '知识库,安全知识,平台帮助', '1'),
  ('E052', 'PLATFORM_HELP', '设备档案是什么，存的是什么数据？', 'PLATFORM_HELP', 'searchPlatformHelp', '设备编号,实验室,状态', '1'),
  ('E053', 'PLATFORM_HELP', '在哪里确认预约草稿？', 'PLATFORM_HELP', 'searchPlatformHelp', '确认提交,预约页面', '1'),
  ('E054', 'PLATFORM_HELP', '这个智能助手能做什么？', 'PLATFORM_HELP', 'searchPlatformHelp', '预约,设备,安全考试,信用分', '1'),
  ('E055', 'PLATFORM_HELP', '预约流程是什么？', 'PLATFORM_HELP', 'searchPlatformHelp', '实验室,时段,确认提交', '1'),
  ('E056', 'PLATFORM_HELP', '安全考试和信用分会怎样影响预约？', 'PLATFORM_HELP', 'searchPlatformHelp', '信用分,安全考试,资格', '1'),
  ('E057', 'PLATFORM_HELP', '实验室档案页面怎么使用？', 'PLATFORM_HELP', 'searchPlatformHelp', '实验室,状态,开放时间', '1'),
  ('E058', 'PLATFORM_HELP', '请直接替我确认预约草稿', 'PLATFORM_HELP', 'searchPlatformHelp', '预约页面,确认', '1'),
  ('E059', 'ROLE_TONE', '我想预约计算机实验室', 'ROOMS_NOW', 'queryAvailableRoomsNow', NULL, '1'),
  ('E060', 'COMPOUND', '现在可以预约哪些实验室和设备？', 'ROOMS_NOW', 'queryAvailableRoomsNow', NULL, '1');

SELECT COUNT(*) AS enabled_case_count
FROM agent_eval_case
WHERE enabled = '1';
