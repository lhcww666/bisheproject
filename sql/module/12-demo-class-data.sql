-- 演示数据：3 个班级、每班 3 名学生、每班 1 名教师和 1 名班级负责人
-- 目的：让实验室准入、班级负责人和班级预约可以直接登录演示。
-- 使用前提：已执行 sql/module/10-room-access.sql、sql/module/11-class-representative.sql。
-- 可重复执行：部门按 dept_id 判重，用户按 user_name 判重，角色和准入规则使用 INSERT IGNORE。
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

-- 兼容旧演示账号：统一改成 stu/teacher 前缀 + 班级顺序编号
UPDATE IGNORE sys_user SET user_name = 'stu101' WHERE user_name IN ('101', 'stu201');
UPDATE IGNORE sys_user SET user_name = 'stu102' WHERE user_name IN ('102', 'stu202');
UPDATE IGNORE sys_user SET user_name = 'stu103' WHERE user_name IN ('103', 'stu203');
UPDATE IGNORE sys_user SET user_name = 'stu201' WHERE user_name IN ('201', 'stu301');
UPDATE IGNORE sys_user SET user_name = 'stu202' WHERE user_name IN ('202', 'stu302');
UPDATE IGNORE sys_user SET user_name = 'stu203' WHERE user_name IN ('203', 'stu303');
UPDATE IGNORE sys_user SET user_name = 'stu301' WHERE user_name IN ('301', 'stu311');
UPDATE IGNORE sys_user SET user_name = 'stu302' WHERE user_name IN ('302', 'stu312');
UPDATE IGNORE sys_user SET user_name = 'stu303' WHERE user_name IN ('303', 'stu313');
UPDATE IGNORE sys_user SET user_name = 'teacher101' WHERE user_name IN ('t101', 'teacher201');
UPDATE IGNORE sys_user SET user_name = 'teacher201' WHERE user_name IN ('t201', 'teacher301');
UPDATE IGNORE sys_user SET user_name = 'teacher301' WHERE user_name IN ('t301', 'teacher302');

-- 1. 部门/班级
DROP PROCEDURE IF EXISTS db_demo_add_dept;
DELIMITER $$
CREATE PROCEDURE db_demo_add_dept(IN p_id BIGINT, IN p_parent BIGINT, IN p_ancestors VARCHAR(50),
                                  IN p_name VARCHAR(30), IN p_order INT, IN p_leader VARCHAR(20))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = p_id) THEN
    INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time)
    VALUES (p_id, p_parent, p_ancestors, p_name, p_order, p_leader, '0', '0', 'admin', sysdate());
  ELSE
    UPDATE sys_dept
    SET parent_id = p_parent, ancestors = p_ancestors, dept_name = p_name, order_num = p_order,
        leader = p_leader, status = '0', del_flag = '0'
    WHERE dept_id = p_id;
  END IF;
END$$
DELIMITER ;

CALL db_demo_add_dept(200, 100, '0,100', '化学与材料学院', 1, 'teacher101');
CALL db_demo_add_dept(201, 200, '0,100,200', '化学2401班', 1, 'teacher101');
CALL db_demo_add_dept(300, 100, '0,100', '信息工程学院', 2, 'teacher201');
CALL db_demo_add_dept(301, 300, '0,100,300', '计算机2401班', 1, 'teacher201');
CALL db_demo_add_dept(302, 300, '0,100,300', '软件2401班', 2, 'teacher301');
CALL db_demo_add_dept(400, 100, '0,100', '生命科学学院', 3, 'teacher401');

DROP PROCEDURE IF EXISTS db_demo_add_dept;

-- 2. 用户：密码沿用现有演示账号，不在脚本中写明文密码
SET @demo_password := COALESCE(
  (SELECT password FROM sys_user WHERE user_name = 'student01' LIMIT 1),
  (SELECT password FROM sys_user WHERE user_name = 'admin' LIMIT 1)
);

DROP PROCEDURE IF EXISTS db_demo_add_user;
DELIMITER $$
CREATE PROCEDURE db_demo_add_user(IN p_dept BIGINT, IN p_name VARCHAR(30), IN p_nick VARCHAR(30),
                                  IN p_password VARCHAR(100), IN p_remark VARCHAR(500))
BEGIN
  IF NOT EXISTS (SELECT 1 FROM sys_user WHERE user_name = p_name COLLATE utf8mb4_general_ci) THEN
    INSERT INTO sys_user (dept_id, user_name, nick_name, user_type, password, status, del_flag, create_by, create_time, remark)
    VALUES (p_dept, p_name, p_nick, '00', p_password, '0', '0', 'admin', sysdate(), p_remark);
  ELSE
    UPDATE sys_user SET dept_id = p_dept, nick_name = p_nick, status = '0', del_flag = '0'
    WHERE user_name = p_name COLLATE utf8mb4_general_ci;
  END IF;
END$$
DELIMITER ;

CALL db_demo_add_user(201, 'stu101', '化学2401张三', @demo_password, '演示学生：化学2401班');
CALL db_demo_add_user(201, 'stu102', '化学2401李四', @demo_password, '演示学生：化学2401班');
CALL db_demo_add_user(201, 'stu103', '化学2401王五', @demo_password, '演示学生：化学2401班');
CALL db_demo_add_user(301, 'stu201', '计算机2401张三', @demo_password, '演示学生：计算机2401班');
CALL db_demo_add_user(301, 'stu202', '计算机2401李四', @demo_password, '演示学生：计算机2401班');
CALL db_demo_add_user(301, 'stu203', '计算机2401王五', @demo_password, '演示学生：计算机2401班');
CALL db_demo_add_user(302, 'stu301', '软件2401张三', @demo_password, '演示学生：软件2401班');
CALL db_demo_add_user(302, 'stu302', '软件2401李四', @demo_password, '演示学生：软件2401班');
CALL db_demo_add_user(302, 'stu303', '软件2401王五', @demo_password, '演示学生：软件2401班');

CALL db_demo_add_user(201, 'teacher101', '化学2401教师', @demo_password, '演示教师：化学2401班');
CALL db_demo_add_user(301, 'teacher201', '计算机2401教师', @demo_password, '演示教师：计算机2401班');
CALL db_demo_add_user(302, 'teacher301', '软件2401教师', @demo_password, '演示教师：软件2401班');

DROP PROCEDURE IF EXISTS db_demo_add_user;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u JOIN sys_role r ON r.role_key = 'student'
WHERE u.user_name IN ('stu101', 'stu102', 'stu103', 'stu201', 'stu202', 'stu203', 'stu301', 'stu302', 'stu303');

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.user_id, r.role_id
FROM sys_user u JOIN sys_role r ON r.role_key = 'teacher'
WHERE u.user_name IN ('teacher101', 'teacher201', 'teacher301');

-- 3. 实验室归属和预约模式
UPDATE room SET dept_id = 200, reservation_mode = 'RESTRICTED' WHERE room_code = 'A101';
UPDATE room SET dept_id = 200, reservation_mode = 'APPROVAL_ONLY' WHERE room_code = 'C301';
UPDATE room SET dept_id = 300, reservation_mode = 'RESTRICTED' WHERE room_code = 'D102';
UPDATE room SET dept_id = 300, reservation_mode = 'RESTRICTED' WHERE room_code = 'E105';
UPDATE room SET dept_id = 400, reservation_mode = 'OPEN' WHERE room_code = 'B203';

-- 清理 10 号脚本里“总公司可预约”的演示规则，换成班级规则
DELETE ra FROM room_access ra
JOIN room r ON r.id = ra.room_id
WHERE ra.remark LIKE '演示%' AND r.room_code IN ('A101', 'C301', 'D102', 'E105');

INSERT IGNORE INTO room_access (room_id, subject_type, subject_id, create_by, create_time, remark)
SELECT r.id, 'CLASS', 201, 'admin', sysdate(), '演示：化学2401班可预约'
FROM room r WHERE r.room_code IN ('A101', 'C301');

INSERT IGNORE INTO room_access (room_id, subject_type, subject_id, create_by, create_time, remark)
SELECT r.id, 'CLASS', 301, 'admin', sysdate(), '演示：计算机2401班可预约'
FROM room r WHERE r.room_code = 'D102';

INSERT IGNORE INTO room_access (room_id, subject_type, subject_id, create_by, create_time, remark)
SELECT r.id, 'CLASS', 302, 'admin', sysdate(), '演示：软件2401班可预约'
FROM room r WHERE r.room_code = 'E105';

-- 4. 班级负责人
DELETE FROM class_representative WHERE dept_id = 100 AND remark LIKE '演示%';

INSERT IGNORE INTO class_representative (dept_id, student_id, status, create_by, create_time, remark)
SELECT 201, u.user_id, '0', 'admin', sysdate(), '演示：化学2401班负责人'
FROM sys_user u WHERE u.user_name = 'stu101';

INSERT IGNORE INTO class_representative (dept_id, student_id, status, create_by, create_time, remark)
SELECT 301, u.user_id, '0', 'admin', sysdate(), '演示：计算机2401班负责人'
FROM sys_user u WHERE u.user_name = 'stu201';

INSERT IGNORE INTO class_representative (dept_id, student_id, status, create_by, create_time, remark)
SELECT 302, u.user_id, '0', 'admin', sysdate(), '演示：软件2401班负责人'
FROM sys_user u WHERE u.user_name = 'stu301';

-- 5. 安全考试与初始信用分，保证高风险实验室也能演示
INSERT INTO safety_exam_record (student_id, score, passed, exam_time, valid_until, create_by, create_time, remark)
SELECT u.user_id, 92.00, '1', sysdate(), DATE_ADD(sysdate(), INTERVAL 365 DAY), 'demo', sysdate(), 'demo-class-seed'
FROM sys_user u
WHERE u.user_name IN ('stu101', 'stu102', 'stu103', 'stu201', 'stu202', 'stu203', 'stu301', 'stu302', 'stu303')
  AND NOT EXISTS (SELECT 1 FROM safety_exam_record s WHERE s.student_id = u.user_id AND s.remark = 'demo-class-seed');

INSERT INTO credit_record (user_id, change_score, balance_score, reason, create_by, create_time, remark)
SELECT u.user_id, 100, 100, '初始信用分', 'demo', sysdate(), 'demo-class-seed'
FROM sys_user u
WHERE u.user_name IN ('stu101', 'stu102', 'stu103', 'stu201', 'stu202', 'stu203', 'stu301', 'stu302', 'stu303')
  AND NOT EXISTS (SELECT 1 FROM credit_record c WHERE c.user_id = u.user_id);

-- 6. 自检
SELECT d.dept_id, d.dept_name,
       COUNT(DISTINCT CASE WHEN r.role_key = 'student' THEN u.user_id END) AS student_count,
       COUNT(DISTINCT CASE WHEN r.role_key = 'teacher' THEN u.user_id END) AS teacher_count
FROM sys_dept d
LEFT JOIN sys_user u ON u.dept_id = d.dept_id
LEFT JOIN sys_user_role ur ON ur.user_id = u.user_id
LEFT JOIN sys_role r ON r.role_id = ur.role_id
WHERE d.dept_id IN (201, 301, 302)
GROUP BY d.dept_id, d.dept_name
ORDER BY d.dept_id;

SELECT r.room_code, r.reservation_mode, ra.subject_type, ra.subject_id, ra.remark
FROM room r LEFT JOIN room_access ra ON ra.room_id = r.id
ORDER BY r.id;

SELECT cr.dept_id, d.dept_name, u.user_name, u.nick_name
FROM class_representative cr
JOIN sys_dept d ON d.dept_id = cr.dept_id
JOIN sys_user u ON u.user_id = cr.student_id
ORDER BY cr.dept_id;
