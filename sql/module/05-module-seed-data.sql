-- 实验室业务模块示例（演示）数据
-- 目的：给设备、预约、审核、安全考试、信用分这 6 张空表造几条符合业务规则的数据，方便页面演示和后续联调。
-- 使用前提：已按顺序执行 sql/ruoyi 基础脚本、sql/module_core.sql（room 表里已有 A101 / B203 / C301 三条实验室）、
--           sql/module/04-module-audit-columns.sql（补齐审计字段，否则本脚本里的 create_time 会报未知列）。
-- 可重复执行：示例数据统一使用 9001-9099 的固定主键，脚本每次执行会先清掉这一段再重新插入。
-- 说明：主键取 9001 起的目的是把“示例数据”和正式数据区分开，自增序号会从 9011 往后继续，不影响正常使用。
-- 数据规则：设备属于对应实验室；预约时间落在实验室开放时段内且互不冲突；参与人数不超过实验室最大人数；
--          已通过/已驳回的预约才有审核记录；安全考试同时给出“有效”和“已过期”两种情况。
USE bishe_schema;

-- 1. 清理上一次的示例数据（先删子表，再删主表，避免外键报错）
DELETE FROM reservation_device WHERE id BETWEEN 9001 AND 9099;
DELETE FROM reservation_review WHERE id BETWEEN 9001 AND 9099;
DELETE FROM reservation       WHERE id BETWEEN 9001 AND 9099;
DELETE FROM device            WHERE id BETWEEN 9001 AND 9099;
DELETE FROM safety_exam_record WHERE id BETWEEN 9001 AND 9099;
DELETE FROM credit_record     WHERE id BETWEEN 9001 AND 9099;
DELETE FROM room              WHERE id BETWEEN 9001 AND 9099;

-- 2. 实验室：补齐“电子实验室”“计算机实验室”两种类型（原有 A101/B203/C301 保持不变）
INSERT INTO room (id, room_code, room_name, room_type, location, capacity, safety_level,
                  open_start_time, open_end_time, status, description, create_by, create_time)
VALUES
(9001, 'D102', '电子实验室', 'ELECTRONICS', '实验楼 D 区 102', 30, 1, '08:00:00', '22:00:00', '0', '用于电子电路与嵌入式开发实验', 'admin', sysdate()),
(9002, 'E105', '计算机实验室', 'COMPUTER', '实验楼 E 区 105', 40, 1, '08:00:00', '22:00:00', '0', '用于计算机类上机实验', 'admin', sysdate());

SET @room_a101 := (SELECT id FROM room WHERE room_code = 'A101');
SET @room_b203 := (SELECT id FROM room WHERE room_code = 'B203');
SET @room_c301 := (SELECT id FROM room WHERE room_code = 'C301');

-- 3. 设备：覆盖计量分析、分离提纯、加热控温、显微观察等类型，并包含维护/停用两种情况
INSERT INTO device (id, room_id, device_code, device_name, device_type, quantity, status, requires_training, create_by, create_time, remark)
VALUES
(9001, @room_a101, 'DEMO-A101-01', '电子天平',       'MEASURE',    2, '0', '0', 'admin', sysdate(), '精度 0.1mg，放在称量台使用'),
(9002, @room_a101, 'DEMO-A101-02', '旋转蒸发仪',     'SEPARATE',   1, '0', '1', 'admin', sysdate(), '操作需佩戴护目镜'),
(9003, @room_a101, 'DEMO-A101-03', '恒温水浴锅',     'HEATING',    3, '1', '0', 'admin', sysdate(), '其中一台温控异常，送修中'),
(9004, @room_b203, 'DEMO-B203-01', '高速离心机',     'SEPARATE',   1, '0', '1', 'admin', sysdate(), '最高 15000rpm，必须对称配平'),
(9005, @room_b203, 'DEMO-B203-02', '光学显微镜',     'MICROSCOPE', 6, '0', '0', 'admin', sysdate(), ''),
(9006, @room_c301, 'DEMO-C301-01', '管式炉',         'HEATING',    1, '0', '1', 'admin', sysdate(), '最高 1200℃，须有教师在场'),
(9007, @room_c301, 'DEMO-C301-02', '万能材料试验机', 'OTHER',      1, '2', '1', 'admin', sysdate(), '传感器待校准，暂停开放'),
(9008, 9001,       'DEMO-D102-01', '数字示波器',     'MEASURE',    4, '0', '0', 'admin', sysdate(), ''),
(9009, 9001,       'DEMO-D102-02', '直流稳压电源',   'OTHER',      6, '0', '0', 'admin', sysdate(), ''),
(9010, 9002,       'DEMO-E105-01', 'GPU 工作站',     'OTHER',      8, '0', '0', 'admin', sysdate(), '开课前需预约，禁止私装软件');

-- 4. 预约：状态覆盖待审核、已通过、已驳回、已取消、已完成；时间相对当前日期生成，反复执行依然“新鲜”
INSERT INTO reservation (id, reservation_no, applicant_id, room_id, experiment_name, start_time, end_time,
                         participant_count, risk_level, status, review_required, remark, create_by, create_time)
VALUES
(9001, 'DEMO-RES-001', 100, @room_a101, '有机溶剂萃取实验',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00'),
 8, 1, '1', '1', '使用旋转蒸发仪和电子天平，需佩戴护目镜', 'admin', DATE_ADD(NOW(), INTERVAL -3 DAY)),

(9002, 'DEMO-RES-002', 100, @room_b203, '细胞离心分离实验',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '16:00:00'),
 12, 2, '0', '1', '需要高速离心机，请老师审核', 'admin', DATE_ADD(NOW(), INTERVAL -2 DAY)),

(9003, 'DEMO-RES-003', 100, @room_c301, '陶瓷材料烧结实验',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '10:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '12:00:00'),
 4, 3, '2', '1', '高温实验，需教师全程在场', 'admin', DATE_ADD(NOW(), INTERVAL -5 DAY)),

(9004, 'DEMO-RES-004', 1, 9001, '嵌入式电路测试',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '13:30:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '15:00:00'),
 6, 1, '3', '0', '临时改期，先取消本次预约', 'admin', DATE_ADD(NOW(), INTERVAL -1 DAY)),

(9005, 'DEMO-RES-005', 100, 9002, '数据库课程设计上机',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '11:00:00'),
 5, 1, '4', '0', '已完成，设备正常归还', 'admin', DATE_ADD(NOW(), INTERVAL -7 DAY)),

(9006, 'DEMO-RES-006', 1, @room_a101, '分析化学称量实验',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '16:00:00'),
 10, 1, '0', '1', '与同日上午的萃取实验不冲突（不同时段）', 'admin', DATE_ADD(NOW(), INTERVAL -1 DAY));

-- 5. 预约设备关联：设备与预约必须同属一个实验室，使用数量不得超过设备数量
INSERT INTO reservation_device (id, reservation_id, device_id, quantity, create_by, create_time)
VALUES
(9001, 9001, 9001, 1, 'admin', DATE_ADD(NOW(), INTERVAL -3 DAY)),  -- 有机溶剂萃取 -> 电子天平
(9002, 9001, 9002, 1, 'admin', DATE_ADD(NOW(), INTERVAL -3 DAY)),  -- 有机溶剂萃取 -> 旋转蒸发仪
(9003, 9002, 9004, 1, 'admin', DATE_ADD(NOW(), INTERVAL -2 DAY)),  -- 细胞离心分离 -> 高速离心机
(9004, 9003, 9006, 1, 'admin', DATE_ADD(NOW(), INTERVAL -5 DAY)),  -- 陶瓷烧结     -> 管式炉
(9005, 9004, 9008, 2, 'admin', DATE_ADD(NOW(), INTERVAL -1 DAY)),  -- 嵌入式电路   -> 数字示波器
(9006, 9005, 9010, 5, 'admin', DATE_ADD(NOW(), INTERVAL -7 DAY));  -- 课程设计上机 -> GPU 工作站

-- 6. 预约审核：只有“已通过”“已驳回”的预约有审核记录（一条预约最多一条）
INSERT INTO reservation_review (id, reservation_id, reviewer_id, result, review_comment, review_time, create_by, create_time)
VALUES
(9001, 9001, 1, '1', '设备与安全资质齐全，同意按申请时间使用', DATE_ADD(NOW(), INTERVAL -3 DAY), 'admin', DATE_ADD(NOW(), INTERVAL -3 DAY)),
(9002, 9003, 1, '2', '高风险高温实验，需补充安全预案并经指导教师签字后重新提交', DATE_ADD(NOW(), INTERVAL -4 DAY), 'admin', DATE_ADD(NOW(), INTERVAL -4 DAY));

-- 7. 安全考试记录：包含“已通过且在有效期内”“未通过”“已过期”三种情况
INSERT INTO safety_exam_record (id, student_id, score, passed, exam_time, valid_until, create_by, create_time)
VALUES
(9001, 100, 92.50, '1', DATE_ADD(NOW(), INTERVAL -60 DAY),  DATE_ADD(NOW(), INTERVAL 305 DAY), 'admin', DATE_ADD(NOW(), INTERVAL -60 DAY)),
(9002, 1,   58.00, '0', DATE_ADD(NOW(), INTERVAL -90 DAY),  NULL,                              'admin', DATE_ADD(NOW(), INTERVAL -90 DAY)),
(9003, 100, 76.00, '1', DATE_ADD(NOW(), INTERVAL -400 DAY), DATE_ADD(NOW(), INTERVAL -35 DAY), 'admin', DATE_ADD(NOW(), INTERVAL -400 DAY));

-- 8. 信用分流水：分值首尾相接（100 -> 95 -> 98、100 -> 90），最后一条的“变动后分值”就是当前信用分
INSERT INTO credit_record (id, user_id, change_score, balance_score, reason, create_time)
VALUES
(9001, 100, 100, 100, '初始信用分', DATE_ADD(NOW(), INTERVAL -120 DAY)),
(9002, 100,  -5,  95, '预约后未按时签到，扣除信用分', DATE_ADD(NOW(), INTERVAL -30 DAY)),
(9003, 100,   3,  98, '按时完成实验并归还设备，奖励信用分', DATE_ADD(NOW(), INTERVAL -10 DAY)),
(9004, 1,   100, 100, '初始信用分', DATE_ADD(NOW(), INTERVAL -120 DAY)),
(9005, 1,   -10,  90, '未经许可取消预约，扣除信用分', DATE_ADD(NOW(), INTERVAL -5 DAY));

-- 9. 执行结果自检：应该看到 room 5、device 10、reservation 6、reservation_device 6、reservation_review 2、safety_exam_record 3、credit_record 5
SELECT 'room' AS 表名, COUNT(*) AS 总条数, SUM(id BETWEEN 9001 AND 9099) AS 示例条数 FROM room
UNION ALL SELECT 'device', COUNT(*), SUM(id BETWEEN 9001 AND 9099) FROM device
UNION ALL SELECT 'reservation', COUNT(*), SUM(id BETWEEN 9001 AND 9099) FROM reservation
UNION ALL SELECT 'reservation_device', COUNT(*), SUM(id BETWEEN 9001 AND 9099) FROM reservation_device
UNION ALL SELECT 'reservation_review', COUNT(*), SUM(id BETWEEN 9001 AND 9099) FROM reservation_review
UNION ALL SELECT 'safety_exam_record', COUNT(*), SUM(id BETWEEN 9001 AND 9099) FROM safety_exam_record
UNION ALL SELECT 'credit_record', COUNT(*), SUM(id BETWEEN 9001 AND 9099) FROM credit_record;

-- 需要清空示例数据时，按下面的顺序单独执行（先子表后主表）：
-- DELETE FROM reservation_device WHERE id BETWEEN 9001 AND 9099;
-- DELETE FROM reservation_review WHERE id BETWEEN 9001 AND 9099;
-- DELETE FROM reservation        WHERE id BETWEEN 9001 AND 9099;
-- DELETE FROM device             WHERE id BETWEEN 9001 AND 9099;
-- DELETE FROM safety_exam_record WHERE id BETWEEN 9001 AND 9099;
-- DELETE FROM credit_record      WHERE id BETWEEN 9001 AND 9099;
-- DELETE FROM room               WHERE id BETWEEN 9001 AND 9099;
