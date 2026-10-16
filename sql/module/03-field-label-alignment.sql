-- 字段注释与页面标签对齐脚本
-- 背景：代码生成器直接使用 MySQL 的 COLUMN COMMENT 作为页面标签，
--       原来的“申请人，对应若依 sys_user.user_id”这类注释会变成过长的表格列名。
-- 作用：把 7 张业务表的注释统一成“短标签 + 括号补说明”的写法，已建库的数据库执行本脚本即可同步。
-- 使用前提：已执行 sql/module_core.sql 建表。
USE bishe_schema;

-- 实验室档案表
ALTER TABLE room MODIFY COLUMN room_name VARCHAR(100) NOT NULL COMMENT '实验室名称';
ALTER TABLE room MODIFY COLUMN location VARCHAR(100) NOT NULL COMMENT '位置';
ALTER TABLE room MODIFY COLUMN capacity INT NOT NULL COMMENT '最大人数';
ALTER TABLE room MODIFY COLUMN open_start_time TIME NOT NULL DEFAULT '08:00:00' COMMENT '开放开始时间';
ALTER TABLE room MODIFY COLUMN open_end_time TIME NOT NULL DEFAULT '21:00:00' COMMENT '开放结束时间';
ALTER TABLE room MODIFY COLUMN description VARCHAR(500) DEFAULT NULL COMMENT '实验室说明';

-- 设备档案表
ALTER TABLE device MODIFY COLUMN room_id BIGINT NOT NULL COMMENT '所属实验室ID';
ALTER TABLE device MODIFY COLUMN device_code VARCHAR(30) NOT NULL COMMENT '设备编号（业务编码，全局唯一）';
ALTER TABLE device MODIFY COLUMN device_name VARCHAR(100) NOT NULL COMMENT '设备名称';
ALTER TABLE device MODIFY COLUMN device_type VARCHAR(50) NOT NULL COMMENT '设备类型';
ALTER TABLE device MODIFY COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '数量';
ALTER TABLE device MODIFY COLUMN requires_training CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否需要培训';

-- 实验室预约表
ALTER TABLE reservation MODIFY COLUMN reservation_no VARCHAR(32) NOT NULL COMMENT '预约编号';
ALTER TABLE reservation MODIFY COLUMN applicant_id BIGINT NOT NULL COMMENT '申请人ID（若依用户主键 sys_user.user_id）';
ALTER TABLE reservation MODIFY COLUMN room_id BIGINT NOT NULL COMMENT '实验室';
ALTER TABLE reservation MODIFY COLUMN experiment_name VARCHAR(100) NOT NULL COMMENT '实验名称';
ALTER TABLE reservation MODIFY COLUMN participant_count INT NOT NULL COMMENT '参与人数';
ALTER TABLE reservation MODIFY COLUMN review_required CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否需要审核';
ALTER TABLE reservation MODIFY COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT '实验说明';

-- 预约设备关联表
ALTER TABLE reservation_device MODIFY COLUMN reservation_id BIGINT NOT NULL COMMENT '预约ID';
ALTER TABLE reservation_device MODIFY COLUMN device_id BIGINT NOT NULL COMMENT '设备ID';
ALTER TABLE reservation_device MODIFY COLUMN quantity INT NOT NULL DEFAULT 1 COMMENT '使用数量';

-- 预约审核表
ALTER TABLE reservation_review MODIFY COLUMN reservation_id BIGINT NOT NULL COMMENT '预约ID';
ALTER TABLE reservation_review MODIFY COLUMN reviewer_id BIGINT NOT NULL COMMENT '审核人ID（若依用户主键 sys_user.user_id）';
ALTER TABLE reservation_review MODIFY COLUMN review_comment VARCHAR(500) DEFAULT NULL COMMENT '审核意见';

-- 安全考试记录表
ALTER TABLE safety_exam_record MODIFY COLUMN student_id BIGINT NOT NULL COMMENT '学生ID（若依用户主键 sys_user.user_id）';
ALTER TABLE safety_exam_record MODIFY COLUMN score DECIMAL(5,2) NOT NULL COMMENT '考试成绩';
ALTER TABLE safety_exam_record MODIFY COLUMN valid_until DATETIME DEFAULT NULL COMMENT '有效期至';

-- 信用分流水表
ALTER TABLE credit_record MODIFY COLUMN user_id BIGINT NOT NULL COMMENT '用户ID（若依用户主键 sys_user.user_id）';
ALTER TABLE credit_record MODIFY COLUMN change_score INT NOT NULL COMMENT '变动分值（可正可负）';
ALTER TABLE credit_record MODIFY COLUMN balance_score INT NOT NULL COMMENT '变动后分值';
