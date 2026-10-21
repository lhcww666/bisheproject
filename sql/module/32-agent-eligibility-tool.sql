-- Agent 综合预约资格工具评测用例。
-- 使用前提：已执行 sql/module/29-agent-eval.sql、sql/module/31-agent-eval-heldout.sql。
-- 可重复执行。
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

UPDATE agent_eval_case
SET category = 'ELIGIBILITY',
    expected_intent = 'ELIGIBILITY',
    expected_tool = 'checkMyReservationEligibility',
    expected_keywords = NULL
WHERE case_code IN ('E005', 'E006');

INSERT IGNORE INTO agent_eval_case
(case_code, category, question, expected_intent, expected_tool, expected_keywords, enabled) VALUES
('E051', 'ELIGIBILITY', '我信用分够，但没有通过安全考试，还可以预约实验室吗？', 'ELIGIBILITY', 'checkMyReservationEligibility', NULL, '1'),
('E052', 'ELIGIBILITY', '安全考试不合格还能预约低风险实验室吗？', 'ELIGIBILITY', 'checkMyReservationEligibility', NULL, '1'),
('E053', 'ELIGIBILITY', '帮我看看我的预约资格。', 'ELIGIBILITY', 'checkMyReservationEligibility', NULL, '1'),
('E054', 'ELIGIBILITY', '我现在能不能预约实验室？', 'ELIGIBILITY', 'checkMyReservationEligibility', NULL, '1'),
('E055', 'ELIGIBILITY', '信用分低于60还能不能预约？', 'ELIGIBILITY', 'checkMyReservationEligibility', NULL, '1');

SELECT COUNT(*) AS enabled_case_count
FROM agent_eval_case
WHERE enabled = '1';
