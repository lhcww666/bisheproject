-- 校园实验室智能预约与安全管理平台：第一版数据库结构
CREATE DATABASE IF NOT EXISTS lab_ai_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lab_ai_platform;

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt 密文',
  real_name VARCHAR(50) NOT NULL,
  student_no VARCHAR(30) DEFAULT NULL,
  phone VARCHAR(20) DEFAULT NULL,
  email VARCHAR(100) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  credit_score INT NOT NULL DEFAULT 100,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_username (username),
  UNIQUE KEY uk_user_student_no (student_no)
) ENGINE=InnoDB COMMENT='用户表';

CREATE TABLE sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  role_code VARCHAR(30) NOT NULL,
  role_name VARCHAR(50) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB COMMENT='角色表';

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

CREATE TABLE lab_room (
  id BIGINT NOT NULL AUTO_INCREMENT,
  room_code VARCHAR(30) NOT NULL,
  room_name VARCHAR(100) NOT NULL,
  lab_type VARCHAR(30) NOT NULL,
  location VARCHAR(150) NOT NULL,
  capacity INT NOT NULL,
  safety_level TINYINT NOT NULL DEFAULT 1 COMMENT '1普通 2中风险 3高风险',
  open_time VARCHAR(100) DEFAULT NULL COMMENT '例如：08:00-21:00',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1可用 0停用 2维护',
  description VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_room_code (room_code)
) ENGINE=InnoDB COMMENT='实验室表';

CREATE TABLE lab_device (
  id BIGINT NOT NULL AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  device_code VARCHAR(30) NOT NULL,
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(50) NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1可用 0停用 2维护',
  need_training TINYINT NOT NULL DEFAULT 0,
  instruction VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_device_code (device_code),
  KEY idx_device_room (room_id),
  CONSTRAINT fk_device_room FOREIGN KEY (room_id) REFERENCES lab_room(id)
) ENGINE=InnoDB COMMENT='设备表';

CREATE TABLE safety_question (
  id BIGINT NOT NULL AUTO_INCREMENT,
  question_type VARCHAR(20) NOT NULL COMMENT 'SINGLE/MULTIPLE/JUDGE',
  content TEXT NOT NULL,
  options_json JSON DEFAULT NULL,
  answer VARCHAR(200) NOT NULL,
  analysis VARCHAR(500) DEFAULT NULL,
  score INT NOT NULL DEFAULT 2,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB COMMENT='安全考试题库';

CREATE TABLE safety_exam_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  score DECIMAL(5,2) NOT NULL,
  passed TINYINT NOT NULL COMMENT '1通过 0未通过',
  exam_time DATETIME NOT NULL,
  valid_until DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_exam_student (student_id, valid_until),
  CONSTRAINT fk_exam_student FOREIGN KEY (student_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='安全考试记录';

CREATE TABLE lab_reservation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reservation_no VARCHAR(32) NOT NULL,
  student_id BIGINT NOT NULL,
  lab_room_id BIGINT NOT NULL,
  experiment_name VARCHAR(100) NOT NULL,
  experiment_type VARCHAR(30) NOT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  participant_count INT NOT NULL,
  risk_level TINYINT NOT NULL DEFAULT 1,
  need_teacher_review TINYINT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  remark VARCHAR(500) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_reservation_no (reservation_no),
  KEY idx_reservation_room_time (lab_room_id, start_time, end_time),
  KEY idx_reservation_student_time (student_id, start_time),
  CONSTRAINT fk_reservation_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
  CONSTRAINT fk_reservation_room FOREIGN KEY (lab_room_id) REFERENCES lab_room(id)
) ENGINE=InnoDB COMMENT='预约主表';

CREATE TABLE reservation_device (
  reservation_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  PRIMARY KEY (reservation_id, device_id),
  CONSTRAINT fk_reservation_device_reservation FOREIGN KEY (reservation_id) REFERENCES lab_reservation(id),
  CONSTRAINT fk_reservation_device_device FOREIGN KEY (device_id) REFERENCES lab_device(id)
) ENGINE=InnoDB COMMENT='预约设备关联表';

CREATE TABLE reservation_review (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reservation_id BIGINT NOT NULL,
  reviewer_id BIGINT NOT NULL,
  review_status VARCHAR(20) NOT NULL COMMENT 'APPROVED/REJECTED',
  review_comment VARCHAR(500) DEFAULT NULL,
  review_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_review_reservation (reservation_id),
  CONSTRAINT fk_review_reservation FOREIGN KEY (reservation_id) REFERENCES lab_reservation(id),
  CONSTRAINT fk_review_user FOREIGN KEY (reviewer_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='预约审核表';

CREATE TABLE credit_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  change_score INT NOT NULL,
  balance_after INT NOT NULL,
  reason VARCHAR(200) NOT NULL,
  related_type VARCHAR(30) DEFAULT NULL,
  related_id BIGINT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_credit_user_time (user_id, create_time),
  CONSTRAINT fk_credit_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='信用分流水';

CREATE TABLE lab_material (
  id BIGINT NOT NULL AUTO_INCREMENT,
  material_code VARCHAR(30) NOT NULL,
  material_name VARCHAR(100) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  stock_quantity DECIMAL(12,2) NOT NULL DEFAULT 0,
  safety_level TINYINT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_code (material_code)
) ENGINE=InnoDB COMMENT='耗材表';

CREATE TABLE material_apply (
  id BIGINT NOT NULL AUTO_INCREMENT,
  reservation_id BIGINT DEFAULT NULL,
  applicant_id BIGINT NOT NULL,
  material_id BIGINT NOT NULL,
  apply_quantity DECIMAL(12,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_material_apply_reservation FOREIGN KEY (reservation_id) REFERENCES lab_reservation(id),
  CONSTRAINT fk_material_apply_user FOREIGN KEY (applicant_id) REFERENCES sys_user(id),
  CONSTRAINT fk_material_apply_material FOREIGN KEY (material_id) REFERENCES lab_material(id)
) ENGINE=InnoDB COMMENT='耗材领用申请';

CREATE TABLE chat_session (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_chat_session_user (user_id),
  CONSTRAINT fk_chat_session_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
) ENGINE=InnoDB COMMENT='AI 会话表';

CREATE TABLE chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL COMMENT 'USER/ASSISTANT/SYSTEM',
  content TEXT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_chat_message_session (session_id, create_time),
  CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_session(id)
) ENGINE=InnoDB COMMENT='AI 消息表';

CREATE TABLE tool_call_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  session_id BIGINT DEFAULT NULL,
  tool_name VARCHAR(100) NOT NULL,
  request_json JSON NOT NULL,
  response_json JSON DEFAULT NULL,
  success TINYINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_tool_user_time (user_id, create_time),
  CONSTRAINT fk_tool_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT fk_tool_session FOREIGN KEY (session_id) REFERENCES chat_session(id)
) ENGINE=InnoDB COMMENT='Agent 工具调用审计表';

INSERT INTO sys_role (role_code, role_name) VALUES
  ('STUDENT', '学生'), ('TEACHER', '指导教师'), ('ADMIN', '实验室管理员');
