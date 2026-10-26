-- 实验室耗材库存、领用申请与库存流水
-- 使用前提：已执行 sql/module_core.sql、sql/module/20-reservation-lifecycle.sql、sql/module/00-module-menu-directory.sql
-- 可重复执行：表、字典、菜单均先判重，角色授权使用 INSERT IGNORE
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS material (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  room_id BIGINT NOT NULL COMMENT '所属实验室ID',
  material_code VARCHAR(30) NOT NULL COMMENT '耗材编号',
  material_name VARCHAR(100) NOT NULL COMMENT '耗材名称',
  specification VARCHAR(100) DEFAULT NULL COMMENT '规格型号',
  unit VARCHAR(20) NOT NULL COMMENT '计量单位',
  stock_quantity DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '当前库存',
  minimum_stock DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '最低库存',
  status CHAR(1) NOT NULL DEFAULT '1' COMMENT '1启用，0停用',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_code (material_code),
  KEY idx_material_room_status (room_id, status),
  CONSTRAINT fk_material_room FOREIGN KEY (room_id) REFERENCES room (id),
  CONSTRAINT chk_material_stock_nonnegative CHECK (stock_quantity >= 0),
  CONSTRAINT chk_material_minimum_nonnegative CHECK (minimum_stock >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室耗材档案';

CREATE TABLE IF NOT EXISTS material_apply (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  apply_no VARCHAR(32) NOT NULL COMMENT '领用申请编号',
  reservation_id BIGINT NOT NULL COMMENT '关联预约ID',
  applicant_id BIGINT NOT NULL COMMENT '申请人用户ID',
  material_id BIGINT NOT NULL COMMENT '耗材ID',
  apply_quantity DECIMAL(12,2) NOT NULL COMMENT '申请数量',
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
    COMMENT 'PENDING待审核，APPROVED已通过，REJECTED已驳回，CANCELED已取消，RETURNED已归还',
  reviewer_id BIGINT DEFAULT NULL COMMENT '审核人用户ID',
  review_comment VARCHAR(500) DEFAULT NULL COMMENT '审核意见',
  review_time DATETIME DEFAULT NULL COMMENT '审核时间',
  issue_time DATETIME DEFAULT NULL COMMENT '实际发放时间',
  return_time DATETIME DEFAULT NULL COMMENT '归还时间',
  create_by VARCHAR(64) DEFAULT '' COMMENT '创建者',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_by VARCHAR(64) DEFAULT '' COMMENT '更新者',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_apply_no (apply_no),
  KEY idx_material_apply_reservation (reservation_id),
  KEY idx_material_apply_applicant_status (applicant_id, status),
  KEY idx_material_apply_material_status (material_id, status),
  CONSTRAINT fk_material_apply_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
  CONSTRAINT fk_material_apply_user FOREIGN KEY (applicant_id) REFERENCES sys_user (user_id),
  CONSTRAINT fk_material_apply_material FOREIGN KEY (material_id) REFERENCES material (id),
  CONSTRAINT fk_material_apply_reviewer FOREIGN KEY (reviewer_id) REFERENCES sys_user (user_id),
  CONSTRAINT chk_material_apply_quantity_positive CHECK (apply_quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室耗材领用申请';

CREATE TABLE IF NOT EXISTS material_stock_record (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  material_id BIGINT NOT NULL COMMENT '耗材ID',
  change_type VARCHAR(20) NOT NULL
    COMMENT 'INITIAL初始入库，ADJUST_IN手工入库，ADJUST_OUT手工出库，APPLY_OUT领用出库，CANCEL_RETURN取消归还',
  change_quantity DECIMAL(12,2) NOT NULL COMMENT '带符号变动数量',
  before_quantity DECIMAL(12,2) NOT NULL COMMENT '变动前库存',
  after_quantity DECIMAL(12,2) NOT NULL COMMENT '变动后库存',
  apply_id BIGINT DEFAULT NULL COMMENT '关联领用申请ID',
  reservation_id BIGINT DEFAULT NULL COMMENT '关联预约ID',
  operator_id BIGINT DEFAULT NULL COMMENT '操作人用户ID',
  operator_name VARCHAR(64) NOT NULL COMMENT '操作人名称快照',
  reason VARCHAR(500) NOT NULL COMMENT '变动原因',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变动时间',
  PRIMARY KEY (id),
  KEY idx_material_stock_material_time (material_id, create_time),
  KEY idx_material_stock_apply (apply_id),
  KEY idx_material_stock_reservation (reservation_id),
  CONSTRAINT fk_material_stock_material FOREIGN KEY (material_id) REFERENCES material (id),
  CONSTRAINT fk_material_stock_apply FOREIGN KEY (apply_id) REFERENCES material_apply (id),
  CONSTRAINT fk_material_stock_reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),
  CONSTRAINT fk_material_stock_operator FOREIGN KEY (operator_id) REFERENCES sys_user (user_id),
  CONSTRAINT chk_material_stock_after_nonnegative CHECK (after_quantity >= 0),
  CONSTRAINT chk_material_stock_quantity_nonzero CHECK (change_quantity <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实验室耗材库存流水';

-- 字典：耗材状态、领用状态和库存变动类型
INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '耗材状态', 'module_material_status', '0', 'admin', sysdate(), '耗材启用和停用状态'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_material_status');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '耗材领用状态', 'module_material_apply_status', '0', 'admin', sysdate(), '耗材领用申请生命周期'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_material_apply_status');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark)
SELECT '耗材库存变动类型', 'module_material_stock_type', '0', 'admin', sysdate(), '耗材库存流水类型'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_type) d WHERE d.dict_type = 'module_material_stock_type');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '启用', '1', 'module_material_status', '', 'success', 'Y', '0', 'admin', sysdate(), '允许申请领用'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_status' AND d.dict_value = '1');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '停用', '0', 'module_material_status', '', 'info', 'N', '0', 'admin', sysdate(), '停止新的领用申请'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_status' AND d.dict_value = '0');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '待审核', 'PENDING', 'module_material_apply_status', '', 'warning', 'Y', '0', 'admin', sysdate(), '等待管理员审核'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_apply_status' AND d.dict_value = 'PENDING');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '已通过', 'APPROVED', 'module_material_apply_status', '', 'success', 'N', '0', 'admin', sysdate(), '已扣减库存并发放'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_apply_status' AND d.dict_value = 'APPROVED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '已驳回', 'REJECTED', 'module_material_apply_status', '', 'danger', 'N', '0', 'admin', sysdate(), '审核未通过或库存不足'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_apply_status' AND d.dict_value = 'REJECTED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '已取消', 'CANCELED', 'module_material_apply_status', '', 'info', 'N', '0', 'admin', sysdate(), '预约取消后关闭未审核申请'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_apply_status' AND d.dict_value = 'CANCELED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '已归还', 'RETURNED', 'module_material_apply_status', '', 'primary', 'N', '0', 'admin', sysdate(), '预约取消后库存已归还'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_apply_status' AND d.dict_value = 'RETURNED');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 1, '初始入库', 'INITIAL', 'module_material_stock_type', '', 'success', 'N', '0', 'admin', sysdate(), '创建耗材时的初始库存'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_stock_type' AND d.dict_value = 'INITIAL');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 2, '手工入库', 'ADJUST_IN', 'module_material_stock_type', '', 'success', 'N', '0', 'admin', sysdate(), '管理员增加库存'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_stock_type' AND d.dict_value = 'ADJUST_IN');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 3, '手工出库', 'ADJUST_OUT', 'module_material_stock_type', '', 'warning', 'N', '0', 'admin', sysdate(), '管理员减少库存'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_stock_type' AND d.dict_value = 'ADJUST_OUT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 4, '领用出库', 'APPLY_OUT', 'module_material_stock_type', '', 'danger', 'N', '0', 'admin', sysdate(), '领用申请审核通过后扣减'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_stock_type' AND d.dict_value = 'APPLY_OUT');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
SELECT 5, '取消归还', 'CANCEL_RETURN', 'module_material_stock_type', '', 'primary', 'N', '0', 'admin', sysdate(), '预约取消后归还库存'
FROM (SELECT 1) t
WHERE NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_dict_data) d
                  WHERE d.dict_type = 'module_material_stock_type' AND d.dict_value = 'CANCEL_RETURN');

-- 演示耗材：按实验室编号绑定，已有相同编号时不覆盖
SET @room_a101 := (SELECT id FROM room WHERE room_code = 'A101' LIMIT 1);
SET @room_b203 := (SELECT id FROM room WHERE room_code = 'B203' LIMIT 1);
SET @room_c301 := (SELECT id FROM room WHERE room_code = 'C301' LIMIT 1);
SET @room_d102 := (SELECT id FROM room WHERE room_code = 'D102' LIMIT 1);
SET @room_e105 := (SELECT id FROM room WHERE room_code = 'E105' LIMIT 1);

INSERT IGNORE INTO material
  (room_id, material_code, material_name, specification, unit, stock_quantity, minimum_stock, status, create_by, create_time, remark)
VALUES
  (@room_a101, 'MAT-A101-001', '一次性丁腈手套', '100只/盒', '盒', 24, 5, '1', 'seed', sysdate(), '化学实验基础防护耗材'),
  (@room_a101, 'MAT-A101-002', '15mL离心管', '100支/包', '包', 12, 3, '1', 'seed', sysdate(), '常规样品分装耗材'),
  (@room_b203, 'MAT-B203-001', '移液枪头', '1000μL/盒', '盒', 18, 4, '1', 'seed', sysdate(), '与实验室移液器配套使用'),
  (@room_c301, 'MAT-C301-001', '金相砂纸', '800目/张', '张', 60, 10, '1', 'seed', sysdate(), '材料样品打磨耗材'),
  (@room_d102, 'MAT-D102-001', '杜邦跳线', '20cm/根', '根', 80, 15, '1', 'seed', sysdate(), '电子电路实验连接线'),
  (@room_e105, 'MAT-E105-001', '网线水晶头', 'RJ45/100只', '盒', 10, 2, '1', 'seed', sysdate(), '网络实验压接线材'),
  (@room_e105, 'MAT-E105-002', '防静电袋', '20x30cm/100只', '包', 16, 3, '1', 'seed', sysdate(), '电子元件收纳防护');

INSERT INTO material_stock_record
  (material_id, change_type, change_quantity, before_quantity, after_quantity, operator_id, operator_name, reason, create_time)
SELECT m.id, 'INITIAL', m.stock_quantity, 0, m.stock_quantity, 1, 'seed', '耗材初始化库存', sysdate()
FROM material m
WHERE m.material_code IN (
  'MAT-A101-001', 'MAT-A101-002', 'MAT-B203-001', 'MAT-C301-001',
  'MAT-D102-001', 'MAT-E105-001', 'MAT-E105-002'
)
  AND NOT EXISTS (
    SELECT 1 FROM material_stock_record r
    WHERE r.material_id = m.id AND r.change_type = 'INITIAL'
  );

-- 菜单：耗材库存、耗材领用、库存流水
SET @moduleMenuId := (SELECT menu_id FROM sys_menu WHERE menu_name = '实验室业务' AND menu_type = 'M' LIMIT 1);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '耗材库存', @moduleMenuId, 12, 'material', 'module/material/list', 1, 0, 'C', '0', '0',
       'module:material:list', 'shopping', 'admin', sysdate(), '实验室耗材档案与库存'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:list');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '耗材领用', @moduleMenuId, 13, 'material-apply', 'module/material/apply', 1, 0, 'C', '0', '0',
       'module:material:apply:list', 'documentation', 'admin', sysdate(), '预约耗材领用申请与审核'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:apply:list');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '库存流水', @moduleMenuId, 14, 'material-stock', 'module/material/stock', 1, 0, 'C', '0', '0',
       'module:material:stock:list', 'list', 'admin', sysdate(), '耗材库存变动审计流水'
FROM (SELECT 1) t
WHERE @moduleMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:stock:list');

UPDATE sys_menu
SET component = 'module/material/list', parent_id = @moduleMenuId, order_num = 12, icon = 'shopping'
WHERE perms = 'module:material:list';
UPDATE sys_menu
SET component = 'module/material/apply', parent_id = @moduleMenuId, order_num = 13, icon = 'documentation'
WHERE perms = 'module:material:apply:list';
UPDATE sys_menu
SET component = 'module/material/stock', parent_id = @moduleMenuId, order_num = 14, icon = 'list'
WHERE perms = 'module:material:stock:list';

SET @materialMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:material:list' LIMIT 1);
SET @materialApplyMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:material:apply:list' LIMIT 1);
SET @materialStockMenuId := (SELECT menu_id FROM sys_menu WHERE perms = 'module:material:stock:list' LIMIT 1);

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '耗材查询', @materialMenuId, 1, '#', '', 1, 0, 'F', '0', '0',
       'module:material:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:query');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '耗材新增', @materialMenuId, 2, '#', '', 1, 0, 'F', '0', '0',
       'module:material:add', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:add');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '耗材修改', @materialMenuId, 3, '#', '', 1, 0, 'F', '0', '0',
       'module:material:edit', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:edit');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '耗材删除', @materialMenuId, 4, '#', '', 1, 0, 'F', '0', '0',
       'module:material:remove', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:remove');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '耗材导出', @materialMenuId, 5, '#', '', 1, 0, 'F', '0', '0',
       'module:material:export', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:export');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '调整库存', @materialMenuId, 6, '#', '', 1, 0, 'F', '0', '0',
       'module:material:adjust', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:adjust');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '领用查询', @materialApplyMenuId, 1, '#', '', 1, 0, 'F', '0', '0',
       'module:material:apply:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialApplyMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:apply:query');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '提交领用', @materialApplyMenuId, 2, '#', '', 1, 0, 'F', '0', '0',
       'module:material:apply', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialApplyMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:apply');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '审核领用', @materialApplyMenuId, 3, '#', '', 1, 0, 'F', '0', '0',
       'module:material:review', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialApplyMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:review');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '领用导出', @materialApplyMenuId, 4, '#', '', 1, 0, 'F', '0', '0',
       'module:material:apply:export', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialApplyMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:apply:export');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '流水查询', @materialStockMenuId, 1, '#', '', 1, 0, 'F', '0', '0',
       'module:material:stock:query', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialStockMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:stock:query');

INSERT INTO sys_menu
  (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '流水导出', @materialStockMenuId, 2, '#', '', 1, 0, 'F', '0', '0',
       'module:material:stock:export', '#', 'admin', sysdate(), ''
FROM (SELECT 1) t
WHERE @materialStockMenuId IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM sys_menu) m WHERE m.perms = 'module:material:stock:export');

-- 角色授权：学生仅申请和查看本人记录，管理员维护与审核全部记录
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN (
  'module:material:apply:list',
  'module:material:apply:query',
  'module:material:apply'
)
WHERE r.role_key = 'student';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN (
  'module:material:list',
  'module:material:query',
  'module:material:add',
  'module:material:edit',
  'module:material:remove',
  'module:material:export',
  'module:material:adjust',
  'module:material:apply:list',
  'module:material:apply:query',
  'module:material:review',
  'module:material:apply:export',
  'module:material:stock:list',
  'module:material:stock:query',
  'module:material:stock:export'
)
WHERE r.role_key = 'admin';

SELECT menu_name, path, component, perms
FROM sys_menu
WHERE perms LIKE 'module:material:%'
ORDER BY parent_id, order_num, menu_id;
