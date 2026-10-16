-- 实验室业务模块示例（演示）数据
-- 目的：给设备、预约、审核、安全考试、信用分这 6 张空表造几条符合业务规则的数据，方便页面演示和后续联调。
-- 使用前提：已按顺序执行 sql/ruoyi 基础脚本、sql/module_core.sql（room 表里已有 A101 / B203 / C301 三条实验室）、
--           sql/module/04-module-audit-columns.sql（补齐审计字段，否则本脚本里的 create_by / create_time 会报未知列）。
-- 可重复执行：本脚本插入的示例行统一带标记 create_by = 'seed'，每次执行先按这个标记清掉上一次的示例数据再重新插入，
--             同时把自增序号拨回起点，所以示例主键是 1、2、3… 这样的连续小数字，不会出现跳号。
-- 业务编号格式：设备编号 EQ-<实验室编号>-<序号>；预约编号 RES<预约日期>-<序号>（例如 RES20261017-001）。
-- 数据规则：设备属于对应实验室；预约时间落在实验室开放时段内且互不冲突；参与人数不超过实验室最大人数；
--          已通过/已驳回的预约才有审核记录；安全考试同时给出“有效”和“已过期”两种情况。
USE bishe_schema;

-- 1. 清理上一次的示例数据（先删子表，再删主表，避免外键报错）
DELETE FROM reservation_device WHERE create_by = 'seed';
DELETE FROM reservation_review WHERE create_by = 'seed';
DELETE FROM reservation        WHERE create_by = 'seed';
DELETE FROM device             WHERE create_by = 'seed';
DELETE FROM safety_exam_record WHERE create_by = 'seed';
DELETE FROM credit_record      WHERE create_by = 'seed';
DELETE FROM room               WHERE create_by = 'seed';

-- 把自增序号拨回起点（表里若已有正式数据，MySQL 会忽略比最大值小的值，不会造成主键冲突）
ALTER TABLE device             AUTO_INCREMENT = 1;
ALTER TABLE reservation        AUTO_INCREMENT = 1;
ALTER TABLE reservation_device AUTO_INCREMENT = 1;
ALTER TABLE reservation_review AUTO_INCREMENT = 1;
ALTER TABLE safety_exam_record AUTO_INCREMENT = 1;
ALTER TABLE credit_record      AUTO_INCREMENT = 1;
ALTER TABLE room               AUTO_INCREMENT = 4;  -- 前面已有 A101 / B203 / C301，新实验室从 4 开始

-- 2. 实验室：补齐“电子实验室”“计算机实验室”两种类型（原有 A101/B203/C301 保持不变）
INSERT INTO room (room_code, room_name, room_type, location, capacity, safety_level,
                  open_start_time, open_end_time, status, description, create_by, create_time)
VALUES
('D102', '电子实验室',   'ELECTRONICS', '实验楼 D 区 102', 30, 1, '08:00:00', '22:00:00', '0', '用于电子电路与嵌入式开发实验', 'seed', sysdate()),
('E105', '计算机实验室', 'COMPUTER',    '实验楼 E 区 105', 40, 1, '08:00:00', '22:00:00', '0', '用于计算机类上机实验',         'seed', sysdate());

SET @room_a101 := (SELECT id FROM room WHERE room_code = 'A101');
SET @room_b203 := (SELECT id FROM room WHERE room_code = 'B203');
SET @room_c301 := (SELECT id FROM room WHERE room_code = 'C301');
SET @room_d102 := (SELECT id FROM room WHERE room_code = 'D102');
SET @room_e105 := (SELECT id FROM room WHERE room_code = 'E105');

-- 3. 设备：覆盖计量分析、分离提纯、加热控温、显微观察等类型，并包含维护/停用两种情况
INSERT INTO device (room_id, device_code, device_name, device_type, quantity, status, requires_training, create_by, create_time, remark)
VALUES
(@room_a101, 'EQ-A101-01', '电子天平',       'MEASURE',    2, '0', '0', 'seed', sysdate(), '精度 0.1mg，放在称量台使用'),
(@room_a101, 'EQ-A101-02', '旋转蒸发仪',     'SEPARATE',   1, '0', '1', 'seed', sysdate(), '操作需佩戴护目镜'),
(@room_a101, 'EQ-A101-03', '恒温水浴锅',     'HEATING',    3, '1', '0', 'seed', sysdate(), '其中一台温控异常，送修中'),
(@room_b203, 'EQ-B203-01', '高速离心机',     'SEPARATE',   1, '0', '1', 'seed', sysdate(), '最高 15000rpm，必须对称配平'),
(@room_b203, 'EQ-B203-02', '光学显微镜',     'MICROSCOPE', 6, '0', '0', 'seed', sysdate(), ''),
(@room_c301, 'EQ-C301-01', '管式炉',         'HEATING',    1, '0', '1', 'seed', sysdate(), '最高 1200℃，须有教师在场'),
(@room_c301, 'EQ-C301-02', '万能材料试验机', 'OTHER',      1, '2', '1', 'seed', sysdate(), '传感器待校准，暂停开放'),
(@room_d102, 'EQ-D102-01', '数字示波器',     'MEASURE',    4, '0', '0', 'seed', sysdate(), ''),
(@room_d102, 'EQ-D102-02', '直流稳压电源',   'OTHER',      6, '0', '0', 'seed', sysdate(), ''),
(@room_e105, 'EQ-E105-01', 'GPU 工作站',     'OTHER',      8, '0', '0', 'seed', sysdate(), '开课前需预约，禁止私装软件');

SET @dev_1  := (SELECT id FROM device WHERE device_code = 'EQ-A101-01');
SET @dev_2  := (SELECT id FROM device WHERE device_code = 'EQ-A101-02');
SET @dev_4  := (SELECT id FROM device WHERE device_code = 'EQ-B203-01');
SET @dev_6  := (SELECT id FROM device WHERE device_code = 'EQ-C301-01');
SET @dev_8  := (SELECT id FROM device WHERE device_code = 'EQ-D102-01');
SET @dev_10 := (SELECT id FROM device WHERE device_code = 'EQ-E105-01');

-- 4. 预约：单号按“RES+预约日期+序号”生成；状态覆盖待审核、已通过、已驳回、已取消、已完成；时间相对当前日期，反复执行依然“新鲜”
SET @res_no_1 := CONCAT('RES', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '%Y%m%d'), '-001');
SET @res_no_2 := CONCAT('RES', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '%Y%m%d'), '-002');
SET @res_no_3 := CONCAT('RES', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '%Y%m%d'), '-003');
SET @res_no_4 := CONCAT('RES', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '%Y%m%d'), '-004');
SET @res_no_5 := CONCAT('RES', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '%Y%m%d'), '-005');
SET @res_no_6 := CONCAT('RES', DATE_FORMAT(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '%Y%m%d'), '-006');

INSERT INTO reservation (reservation_no, applicant_id, room_id, experiment_name, start_time, end_time,
                         participant_count, risk_level, status, review_required, remark, create_by, create_time)
VALUES
(@res_no_1, 100, @room_a101, '有机溶剂萃取实验',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '11:00:00'),
 8, 1, '1', '1', '使用旋转蒸发仪和电子天平，需佩戴护目镜', 'seed', DATE_ADD(NOW(), INTERVAL -3 DAY)),

(@res_no_2, 100, @room_b203, '细胞离心分离实验',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '14:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '16:00:00'),
 12, 2, '0', '1', '需要高速离心机，请老师审核', 'seed', DATE_ADD(NOW(), INTERVAL -2 DAY)),

(@res_no_3, 100, @room_c301, '陶瓷材料烧结实验',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '10:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '12:00:00'),
 4, 3, '2', '1', '高温实验，需教师全程在场', 'seed', DATE_ADD(NOW(), INTERVAL -5 DAY)),

(@res_no_4, 1, @room_d102, '嵌入式电路测试',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '13:30:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '15:00:00'),
 6, 1, '3', '0', '临时改期，先取消本次预约', 'seed', DATE_ADD(NOW(), INTERVAL -1 DAY)),

(@res_no_5, 100, @room_e105, '数据库课程设计上机',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '09:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -1 DAY), '11:00:00'),
 5, 1, '4', '0', '已完成，设备正常归还', 'seed', DATE_ADD(NOW(), INTERVAL -7 DAY)),

(@res_no_6, 1, @room_a101, '分析化学称量实验',
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00:00'),
 TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '16:00:00'),
 10, 1, '0', '1', '与同日上午的萃取实验不冲突（不同时段）', 'seed', DATE_ADD(NOW(), INTERVAL -1 DAY));

SET @res_1 := (SELECT id FROM reservation WHERE reservation_no = @res_no_1);
SET @res_2 := (SELECT id FROM reservation WHERE reservation_no = @res_no_2);
SET @res_3 := (SELECT id FROM reservation WHERE reservation_no = @res_no_3);
SET @res_4 := (SELECT id FROM reservation WHERE reservation_no = @res_no_4);
SET @res_5 := (SELECT id FROM reservation WHERE reservation_no = @res_no_5);

-- 5. 预约设备关联：设备与预约必须同属一个实验室，使用数量不得超过设备数量
INSERT INTO reservation_device (reservation_id, device_id, quantity, create_by, create_time)
VALUES
(@res_1, @dev_1,  1, 'seed', DATE_ADD(NOW(), INTERVAL -3 DAY)),  -- 有机溶剂萃取 -> 电子天平
(@res_1, @dev_2,  1, 'seed', DATE_ADD(NOW(), INTERVAL -3 DAY)),  -- 有机溶剂萃取 -> 旋转蒸发仪
(@res_2, @dev_4,  1, 'seed', DATE_ADD(NOW(), INTERVAL -2 DAY)),  -- 细胞离心分离 -> 高速离心机
(@res_3, @dev_6,  1, 'seed', DATE_ADD(NOW(), INTERVAL -5 DAY)),  -- 陶瓷烧结     -> 管式炉
(@res_4, @dev_8,  2, 'seed', DATE_ADD(NOW(), INTERVAL -1 DAY)),  -- 嵌入式电路   -> 数字示波器
(@res_5, @dev_10, 5, 'seed', DATE_ADD(NOW(), INTERVAL -7 DAY));  -- 课程设计上机 -> GPU 工作站

-- 6. 预约审核：只有“已通过”“已驳回”的预约有审核记录（一条预约最多一条）
INSERT INTO reservation_review (reservation_id, reviewer_id, result, review_comment, review_time, create_by, create_time)
VALUES
(@res_1, 1, '1', '设备与安全资质齐全，同意按申请时间使用', DATE_ADD(NOW(), INTERVAL -3 DAY), 'seed', DATE_ADD(NOW(), INTERVAL -3 DAY)),
(@res_3, 1, '2', '高风险高温实验，需补充安全预案并经指导教师签字后重新提交', DATE_ADD(NOW(), INTERVAL -4 DAY), 'seed', DATE_ADD(NOW(), INTERVAL -4 DAY));

-- 7. 安全考试记录：包含“已通过且在有效期内”“未通过”“已过期”三种情况
INSERT INTO safety_exam_record (student_id, score, passed, exam_time, valid_until, create_by, create_time)
VALUES
(100, 92.50, '1', DATE_ADD(NOW(), INTERVAL -60 DAY),  DATE_ADD(NOW(), INTERVAL 305 DAY), 'seed', DATE_ADD(NOW(), INTERVAL -60 DAY)),
(1,   58.00, '0', DATE_ADD(NOW(), INTERVAL -90 DAY),  NULL,                              'seed', DATE_ADD(NOW(), INTERVAL -90 DAY)),
(100, 76.00, '1', DATE_ADD(NOW(), INTERVAL -400 DAY), DATE_ADD(NOW(), INTERVAL -35 DAY), 'seed', DATE_ADD(NOW(), INTERVAL -400 DAY));

-- 8. 信用分流水：分值首尾相接（100 -> 95 -> 98、100 -> 90），最后一条的“变动后分值”就是当前信用分
INSERT INTO credit_record (user_id, change_score, balance_score, reason, create_time, create_by)
VALUES
(100, 100, 100, '初始信用分', DATE_ADD(NOW(), INTERVAL -120 DAY), 'seed'),
(100,  -5,  95, '预约后未按时签到，扣除信用分', DATE_ADD(NOW(), INTERVAL -30 DAY), 'seed'),
(100,   3,  98, '按时完成实验并归还设备，奖励信用分', DATE_ADD(NOW(), INTERVAL -10 DAY), 'seed'),
(1,   100, 100, '初始信用分', DATE_ADD(NOW(), INTERVAL -120 DAY), 'seed'),
(1,   -10,  90, '未经许可取消预约，扣除信用分', DATE_ADD(NOW(), INTERVAL -5 DAY), 'seed');

-- 9. 执行结果自检：应该看到 room 5、device 10、reservation 6、reservation_device 6、reservation_review 2、safety_exam_record 3、credit_record 5
SELECT 'room' AS 表名, COUNT(*) AS 总条数, SUM(create_by = 'seed') AS 示例条数 FROM room
UNION ALL SELECT 'device', COUNT(*), SUM(create_by = 'seed') FROM device
UNION ALL SELECT 'reservation', COUNT(*), SUM(create_by = 'seed') FROM reservation
UNION ALL SELECT 'reservation_device', COUNT(*), SUM(create_by = 'seed') FROM reservation_device
UNION ALL SELECT 'reservation_review', COUNT(*), SUM(create_by = 'seed') FROM reservation_review
UNION ALL SELECT 'safety_exam_record', COUNT(*), SUM(create_by = 'seed') FROM safety_exam_record
UNION ALL SELECT 'credit_record', COUNT(*), SUM(create_by = 'seed') FROM credit_record;

-- 需要清空示例数据时，按下面的顺序单独执行（先子表后主表；正式数据不会带 create_by = 'seed' 这个标记）：
-- DELETE FROM reservation_device WHERE create_by = 'seed';
-- DELETE FROM reservation_review WHERE create_by = 'seed';
-- DELETE FROM reservation        WHERE create_by = 'seed';
-- DELETE FROM device             WHERE create_by = 'seed';
-- DELETE FROM safety_exam_record WHERE create_by = 'seed';
-- DELETE FROM credit_record      WHERE create_by = 'seed';
-- DELETE FROM room               WHERE create_by = 'seed';
