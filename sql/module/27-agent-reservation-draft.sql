-- Agent 受控预约草稿
-- Agent 只创建草稿；用户在前端点击确认后，后端重新执行业务规则校验并提交预约。
-- 使用前提：已执行 sql/module/24-agent.sql、sql/module/26-agent-rag.sql
-- 可重复执行
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS reservation_draft (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '草稿所属用户',
  session_id BIGINT DEFAULT NULL COMMENT '来源会话',
  room_id BIGINT NOT NULL COMMENT '实验室ID',
  experiment_name VARCHAR(100) NOT NULL COMMENT '实验名称',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  participant_count INT NOT NULL COMMENT '参与人数',
  device_ids TEXT COMMENT '设备ID JSON数组',
  remark VARCHAR(500) DEFAULT NULL COMMENT '实验说明',
  status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/CONFIRMED/CANCELED/EXPIRED',
  reservation_id BIGINT DEFAULT NULL COMMENT '确认后生成的预约ID',
  reservation_no VARCHAR(32) DEFAULT NULL COMMENT '确认后生成的预约编号',
  expires_at DATETIME NOT NULL COMMENT '草稿过期时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_reservation_draft_user (user_id, status, update_time),
  KEY idx_reservation_draft_session (session_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent预约草稿';
