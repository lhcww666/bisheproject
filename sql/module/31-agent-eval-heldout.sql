-- Agent 泛化评测：新增 15 条改写安全问法、4 条业务外问题和 1 条信用分迁移问法。
-- 使用前提：已执行 sql/module/29-agent-eval.sql、sql/module/30-agent-knowledge-expansion.sql。
-- 可重复执行。
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

INSERT IGNORE INTO agent_eval_case
(case_code, category, question, expected_intent, expected_tool, expected_keywords, enabled) VALUES
('E031', 'SAFETY_HELDOUT', '盐酸沾到手上怎么急救？', 'SAFETY', 'searchSafetyKnowledge', '强酸,清水', '1'),
('E032', 'SAFETY_HELDOUT', '氢氧化钠溶液溅到皮肤怎么办？', 'SAFETY', 'searchSafetyKnowledge', '强碱,清水', '1'),
('E033', 'SAFETY_HELDOUT', '水银温度计打碎了怎么处理？', 'SAFETY', 'searchSafetyKnowledge', '水银,硫粉', '1'),
('E034', 'SAFETY_HELDOUT', '酒精洒在实验台上了要怎么办？', 'SAFETY', 'searchSafetyKnowledge', '吸附,通风', '1'),
('E035', 'SAFETY_HELDOUT', '通风柜风速不够还能继续做实验吗？', 'SAFETY', 'searchSafetyKnowledge', '通风柜,停止', '1'),
('E036', 'SAFETY_HELDOUT', '气体钢瓶为什么要用链条固定？', 'SAFETY', 'searchSafetyKnowledge', '钢瓶,固定', '1'),
('E037', 'SAFETY_HELDOUT', '氧气瓶和乙炔瓶能放在一起吗？', 'SAFETY', 'searchSafetyKnowledge', '氧气,乙炔', '1'),
('E038', 'SAFETY_HELDOUT', '试管加热时管口应该朝哪里？', 'SAFETY', 'searchSafetyKnowledge', '管口,加热', '1'),
('E039', 'SAFETY_HELDOUT', '玻璃器皿割破手怎么办？', 'SAFETY', 'searchSafetyKnowledge', '清水,伤口', '1'),
('E040', 'SAFETY_HELDOUT', '从超低温冰箱拿样本要戴什么手套？', 'SAFETY', 'searchSafetyKnowledge', '防冻,护目镜', '1'),
('E041', 'SAFETY_HELDOUT', '烘箱里能不能放含乙醇的玻璃瓶？', 'SAFETY', 'searchSafetyKnowledge', '烘箱,易燃', '1'),
('E042', 'SAFETY_HELDOUT', '实验做到一半突然停电怎么办？', 'SAFETY', 'searchSafetyKnowledge', '停电,设备', '1'),
('E043', 'SAFETY_HELDOUT', '闻到刺鼻气体并且头晕怎么办？', 'SAFETY', 'searchSafetyKnowledge', '新鲜空气,急救', '1'),
('E044', 'SAFETY_HELDOUT', '培养皿打翻有菌液洒出来怎么处理？', 'SAFETY', 'searchSafetyKnowledge', '消毒,泄漏', '1'),
('E045', 'SAFETY_HELDOUT', '被污染针头扎伤后怎么处理？', 'SAFETY', 'searchSafetyKnowledge', '清水,报告', '1'),
('E046', 'OUT_OF_SCOPE', '今天图书馆开放到几点？', 'UNKNOWN', 'unknown', NULL, '1'),
('E047', 'OUT_OF_SCOPE', '怎么申请国家奖学金？', 'UNKNOWN', 'unknown', NULL, '1'),
('E048', 'OUT_OF_SCOPE', '推荐一部好看的科幻电影。', 'UNKNOWN', 'unknown', NULL, '1'),
('E049', 'OUT_OF_SCOPE', '我的银行卡密码是多少？', 'UNKNOWN', 'unknown', NULL, '1'),
('E050', 'CREDIT', '我的违约记录会扣多少信用分？', 'CREDIT', 'queryMyCredit', NULL, '1');

SELECT COUNT(*) AS enabled_case_count
FROM agent_eval_case
WHERE enabled = '1';
