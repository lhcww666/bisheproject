-- Agent RAG 安全知识库
-- 当前阶段使用 MySQL 文本检索基线，后续可将 KnowledgeSearchService 替换为
-- Spring AI VectorStore + EmbeddingModel，表结构和工具接口保持兼容。
-- 使用前提：已执行 sql/module/24-agent.sql、sql/module/25-agent-menu-rename.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS knowledge_document (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  title VARCHAR(200) NOT NULL COMMENT '文档标题',
  category VARCHAR(50) DEFAULT NULL COMMENT '安全知识分类',
  source VARCHAR(300) DEFAULT NULL COMMENT '资料来源或制度名称',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0启用，1停用',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_document_title (title),
  KEY idx_knowledge_document_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent安全知识文档';

CREATE TABLE IF NOT EXISTS knowledge_chunk (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  document_id BIGINT NOT NULL COMMENT '所属文档',
  chunk_no INT NOT NULL DEFAULT 1 COMMENT '文档内片段序号',
  keywords VARCHAR(500) DEFAULT NULL COMMENT '人工维护的检索关键词',
  content TEXT NOT NULL COMMENT '知识片段正文',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '0启用，1停用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_knowledge_chunk (document_id, chunk_no),
  KEY idx_knowledge_chunk_status (status),
  CONSTRAINT fk_knowledge_chunk_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent安全知识片段';

INSERT IGNORE INTO knowledge_document (id, title, category, source, status, create_by, create_time, remark) VALUES
(1001, '化学品眼部接触应急处置', '应急处置', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '眼部接触化学品时的应急处理'),
(1002, '酒精灯与易燃液体火灾处置', '消防', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '酒精灯和小面积易燃液体火灾'),
(1003, '实验室个人防护用品', '个人防护', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '实验服、护目镜和手套'),
(1004, '浓硫酸稀释操作', '化学品安全', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '浓硫酸稀释顺序和防护'),
(1005, '电气火灾与触电急救', '电气安全', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '切断电源、灭火和触电急救'),
(1006, '离心机安全操作', '设备安全', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '离心管配平和盖子检查'),
(1007, '实验废液与危险废物管理', '废弃物管理', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '废液分类、标签和容器要求'),
(1008, '实验室火灾疏散', '消防', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '火灾报警和疏散路线'),
(1009, '高压灭菌锅安全操作', '设备安全', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '水位、密封和冷空气排放'),
(1010, '实验室离室检查', '日常管理', '实验室安全通用规范（演示资料）', '0', 'admin', sysdate(), '水电气、台面和门窗检查');

INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10001, 1001, 1, '化学品 溅入眼睛 眼部 冲洗 清水 15分钟',
 '化学品溅入眼睛后应立即停止实验，避免揉眼，用大量清水或洗眼器持续冲洗至少15分钟，并尽快报告指导教师或实验室管理员。冲洗时注意保护另一只眼睛，不要使用纸巾或毛巾反复擦拭。若化学品具有腐蚀性，应同时查看化学品安全技术说明书，并尽快就医。');
INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10002, 1002, 1, '酒精灯 酒精 着火 起火 湿抹布 灭火毯 明火',
 '少量酒精洒出并着火时，应立即远离火源并报告教师。可使用湿抹布、灭火毯或干粉灭火器覆盖灭火，不能用水直接浇灭酒精火焰，也不能用嘴吹。若火势扩大或引燃其他物品，应立即报警并按疏散路线撤离。');
INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10003, 1003, 1, '实验服 护目镜 手套 个人防护 防护用品',
 '进入化学实验室应穿实验服、戴护目镜，并根据化学品性质选择合适的手套。长发应束起，不得穿拖鞋、短裤或暴露皮肤的服装。实验服只能在实验区域穿着，离开实验室进入食堂等公共区域前应脱下。');
INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10004, 1004, 1, '浓硫酸 稀释 酸性 水 搅拌 放热',
 '稀释浓硫酸时必须将浓硫酸沿容器内壁缓慢倒入水中，并不断搅拌散热，严禁将水倒入浓硫酸中。操作应在通风良好的区域进行，佩戴护目镜、实验服和耐酸碱手套。容器应使用耐热玻璃或指定容器，避免局部过热造成飞溅。');
INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10005, 1005, 1, '电气 电器 着火 火灾 触电 电源 干粉灭火器 绝缘',
 '电器着火时应先切断电源，再使用干粉灭火器灭火，不能用水直接扑救带电设备。发现有人触电时，不能直接用手拉拽，应先切断电源，或用干燥木棒等绝缘物使触电者脱离电源，并立即呼救。');
INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10006, 1006, 1, '离心机 离心管 配平 对称 盖子 超速',
 '使用离心机前应检查离心管是否完好，按对称位置配平，盖好并锁紧转子盖。运行过程中不得打开盖子或移动设备，出现异常振动、异响应立即停机并报告。超速或未配平可能导致离心管破裂和样品飞溅。');
INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10007, 1007, 1, '废液 危险废物 容器 标签 分类 下水道',
 '实验废液应按有机、无机、酸碱和含重金属等类别分类收集，使用有明显标签的密闭容器，不得随意倒入下水道或与生活垃圾混放。');
INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10008, 1008, 1, '火灾 疏散 逃生 电梯 湿毛巾 低姿 撤离',
 '实验室发生火灾时应保持冷静，立即报警并按安全疏散路线撤离。逃生时尽量低姿前进，用湿毛巾捂住口鼻，不要乘坐电梯，也不要返回取个人物品。到达安全集合点后听从现场负责人清点人数。');
INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10009, 1009, 1, '高压灭菌锅 灭菌 水位 密封 冷空气 压力',
 '使用高压灭菌锅前应检查水位、密封圈和排气阀，确认锅内有足够水并排尽冷空气后再升压。运行中不得强行打开盖子，压力未降至安全范围前不得开锅。出现异常压力或泄漏应立即停止使用并报告管理员。');
INSERT IGNORE INTO knowledge_chunk (id, document_id, chunk_no, keywords, content) VALUES (10100, 1010, 1, '离室 检查 水电气 台面 门窗 洗手',
 '实验结束离开实验室前应关闭不必要的水、电、气源，整理实验台面，将试剂和废弃物放回指定位置，检查门窗并洗手。最后离开的人员应确认设备处于安全状态，按规定登记离室信息。');