-- 校园实验室平台核心业务表（最终命名版）
-- 使用前提：已执行若依 ruoyi-mysql.sql，当前数据库为 bishe_schema。
USE bishe_schema;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS reservation_review;
DROP TABLE IF EXISTS reservation_device;
DROP TABLE IF EXISTS reservation;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS room;
DROP TABLE IF EXISTS safety_exam_record;
DROP TABLE IF EXISTS credit_record;

CREATE TABLE room (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  room_code VARCHAR(30) NOT NULL COMMENT '实验室编号',
  room_name VARCHAR(100) NOT NULL COMMENT '实验室名称',
  room_type VARCHAR(30) NOT NULL COMMENT '实验室类型',
  location VARCHAR(100) NOT NULL COMMENT '位置',
  capacity INT NOT NULL COMMENT '最大人数',
  safety_level TINYINT NOT NULL DEFAULT 1 COMMENT '安全等级',
  open_start_time TIME NOT NULL DEFAULT '08:00:00' COMMENT '开放开始时间',
  open_end_time TIME NOT NULL DEFAULT '21:00:00' COMMENT '开放结束时间',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态',
  description VARCHAR(500) DEFAULT NULL COMMENT '说明',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_code (room_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室信息表';

CREATE TABLE device (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  room_id BIGINT NOT NULL COMMENT '所属实验室ID',
  device_code VARCHAR(30) NOT NULL COMMENT '设备编号（业务编码，全局唯一）',
  device_name VARCHAR(100) NOT NULL COMMENT '设备名称',
  device_type VARCHAR(50) NOT NULL COMMENT '设备类型',
  quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '状态',
  requires_training CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否需要培训',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  remark VARCHAR(500) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_code (device_code),
  KEY idx_device_room (room_id),
  CONSTRAINT fk_device_room FOREIGN KEY (room_id) REFERENCES room (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备信息表';

CREATE TABLE reservation (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  reservation_no VARCHAR(32) NOT NULL COMMENT '预约编号',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID（若依用户主键 sys_user.user_id）',
  room_id BIGINT NOT NULL COMMENT '实验室',
  experiment_name VARCHAR(100) NOT NULL COMMENT '实验名称',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  participant_count INT NOT NULL COMMENT '参与人数',
  risk_level TINYINT NOT NULL DEFAULT 1 COMMENT '风险等级',
  status CHAR(1) NOT NULL DEFAULT '0' COMMENT '预约状态',
  review_required CHAR(1) NOT NULL DEFAULT '0' COMMENT '是否需要审核',
  remark VARCHAR(500) DEFAULT NULL COMMENT '实验说明',
  create_by VARCHAR(64) DEFAULT '',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) DEFAULT '',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reservation_no (reservation_no),
  KEY idx_reservation_room_time (room_id, start_time, end_time),
  KEY idx_reservation_applicant (applicant_id, create_time),
  CONSTRAINT fk_reservation_room FOREIGN KEY (room_id) REFERENCES room (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室预约表';

CREATE TABLE reservation_device (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  reservation_id BIGINT NOT NULL COMMENT '预约ID',
  device_id BIGINT NOT NULL COMMENT '设备ID',
  quantity INT NOT NULL DEFAULT 1 COMMENT '使用数量',
  PRIMARY KEY (id),
  UNIQUE KEY uk_reservation_device (reservation_id, device_id),
  CONSTRAINT fk_reservation_device_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
  CONSTRAINT fk_reservation_device_device FOREIGN KEY (device_id) REFERENCES device (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约设备关联表';

CREATE TABLE reservation_review (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  reservation_id BIGINT NOT NULL COMMENT '预约ID',
  reviewer_id BIGINT NOT NULL COMMENT '审核人ID（若依用户主键 sys_user.user_id）',
  result CHAR(1) NOT NULL COMMENT '审核结果',
  review_comment VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  review_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_reservation (reservation_id),
  CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约审核表';

CREATE TABLE safety_exam_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  student_id BIGINT NOT NULL COMMENT '学生ID（若依用户主键 sys_user.user_id）',
  score DECIMAL(5,2) NOT NULL COMMENT '考试成绩',
  passed CHAR(1) NOT NULL COMMENT '是否通过',
  exam_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '考试时间',
  valid_until DATETIME DEFAULT NULL COMMENT '有效期至',
  PRIMARY KEY (id),
  KEY idx_exam_student_valid (student_id, valid_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全考试记录表';

CREATE TABLE credit_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '用户ID（若依用户主键 sys_user.user_id）',
  change_score INT NOT NULL COMMENT '变动分值（可正可负）',
  balance_score INT NOT NULL COMMENT '变动后分值',
  reason VARCHAR(200) NOT NULL COMMENT '变动原因',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '变动时间',
  PRIMARY KEY (id),
  KEY idx_credit_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='信用分流水表';

SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO room (room_code, room_name, room_type, location, capacity, safety_level, description)
VALUES
('A101', '有机化学实验室', 'CHEMISTRY', '实验楼 A 区 101', 24, 2, '用于有机化学相关实验'),
('B203', '生物实验室', 'BIOLOGY', '实验楼 B 区 203', 20, 1, '用于生物实验和基础离心操作'),
('C301', '材料实验室', 'MATERIAL', '实验楼 C 区 301', 16, 3, '涉及材料制备和高风险设备操作');
