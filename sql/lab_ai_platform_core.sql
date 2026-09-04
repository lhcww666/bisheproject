-- 校园实验室平台：核心版数据库（第一期）
-- 命名：表名和字段名使用 snake_case；主键统一 id；时间统一 DATETIME。
CREATE DATABASE IF NOT EXISTS lab_ai_platform
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab_ai_platform;

-- 1. 用户与角色
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  real_name VARCHAR(50) NOT NULL,
  student_no VARCHAR(30) UNIQUE,
  role_code VARCHAR(20) NOT NULL COMMENT 'STUDENT/TEACHER/ADMIN',
  credit_score INT NOT NULL DEFAULT 100,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='用户表';

-- 2. 实验室
CREATE TABLE lab_room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_code VARCHAR(30) NOT NULL UNIQUE,
  room_name VARCHAR(100) NOT NULL,
  room_type VARCHAR(30) NOT NULL COMMENT 'CHEMISTRY/BIOLOGY/PHYSICS/OTHER',
  location VARCHAR(100) NOT NULL,
  capacity INT NOT NULL,
  safety_level TINYINT NOT NULL DEFAULT 1 COMMENT '1普通 2中风险 3高风险',
  open_time VARCHAR(50) NOT NULL DEFAULT '08:00-21:00',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1可用 2维护 0停用',
  description VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='实验室表';

-- 3. 设备
CREATE TABLE lab_device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  device_code VARCHAR(30) NOT NULL UNIQUE,
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(50) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1可用 2维护 0停用',
  requires_training TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_device_room FOREIGN KEY (room_id) REFERENCES lab_room(id)
) ENGINE=InnoDB COMMENT='设备表';

-- 4. 预约主表
CREATE TABLE lab_reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_no VARCHAR(32) NOT NULL UNIQUE,
  applicant_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  experiment_name VARCHAR(100) NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  participant_count INT NOT NULL,
  risk_level TINYINT NOT NULL DEFAULT 1,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING/APPROVED/REJECTED/CANCELED/COMPLETED',
  review_required TINYINT NOT NULL DEFAULT 0,
  remark VARCHAR(500),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_reservation_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user(id),
  CONSTRAINT fk_reservation_room FOREIGN KEY (room_id) REFERENCES lab_room(id),
  KEY idx_reservation_room_time (room_id, start_time, end_time),
  KEY idx_reservation_applicant (applicant_id, created_at)
) ENGINE=InnoDB COMMENT='实验室预约表';

-- 5. 预约设备关联
CREATE TABLE reservation_device (
  reservation_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  PRIMARY KEY (reservation_id, device_id),
  CONSTRAINT fk_reservation_device_reservation FOREIGN KEY (reservation_id) REFERENCES lab_reservation(id),
  CONSTRAINT fk_reservation_device_device FOREIGN KEY (device_id) REFERENCES lab_device(id)
) ENGINE=InnoDB COMMENT='预约设备关联表';

-- 6. 教师审核
CREATE TABLE reservation_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL UNIQUE,
  reviewer_id BIGINT NOT NULL,
  result VARCHAR(20) NOT NULL COMMENT 'APPROVED/REJECTED',
  comment VARCHAR(500),
  reviewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES lab_reservation(id),
  CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='预约审核表';

-- 7. 安全考试记录
CREATE TABLE safety_exam_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  score DECIMAL(5,2) NOT NULL,
  passed TINYINT NOT NULL COMMENT '1通过 0未通过',
  examined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  valid_until DATETIME,
  CONSTRAINT fk_exam_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
  KEY idx_exam_student_valid (student_id, valid_until)
) ENGINE=InnoDB COMMENT='安全考试记录表';

-- 8. 信用分流水
CREATE TABLE credit_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  change_score INT NOT NULL,
  balance_score INT NOT NULL,
  reason VARCHAR(200) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_credit_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  KEY idx_credit_user_time (user_id, created_at)
) ENGINE=InnoDB COMMENT='信用分变动记录表';

-- 初始角色通过 sys_user.role_code 表示，第一期不额外拆分角色表。
-- 后续需要菜单级权限时，再接入若依的 sys_role、sys_menu、sys_user_role 体系。
