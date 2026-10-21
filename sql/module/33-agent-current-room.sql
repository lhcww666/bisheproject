-- Agent 当前时间可约实验室评测用例。
-- 使用前提：已执行 sql/module/29-agent-eval.sql、sql/module/31-agent-eval-heldout.sql。
-- 可重复执行。
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

INSERT IGNORE INTO agent_eval_case
(case_code, category, question, expected_intent, expected_tool, expected_keywords, enabled) VALUES
('E056', 'ROOMS_NOW', '你现在就帮我看看现在还能预约哪个实验室。', 'ROOMS_NOW', 'queryAvailableRoomsNow', NULL, '1'),
('E057', 'ROOMS_NOW', '当前时间还有哪些实验室可用？', 'ROOMS_NOW', 'queryAvailableRoomsNow', NULL, '1'),
('E058', 'ROOMS_NOW', '我现在可以预约哪个实验室？', 'ROOMS_NOW', 'queryAvailableRoomsNow', NULL, '1');

SELECT COUNT(*) AS enabled_case_count
FROM agent_eval_case
WHERE enabled = '1';
