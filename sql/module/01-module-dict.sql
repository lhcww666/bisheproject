-- 实验室业务模块字典：为枚举字段提供下拉选项与标签展示
-- 使用前提：已执行 sql/module_core.sql，并已在代码生成器中导入 7 张业务表
-- 执行顺序：导入表 -> 本脚本 -> 生成代码
USE bishe_schema;

DELETE FROM sys_dict_data WHERE dict_type IN
 ('module_room_status', 'module_device_status', 'module_reservation_status', 'module_room_type', 'module_device_type', 'module_yes_no', 'module_review_result');
DELETE FROM sys_dict_type WHERE dict_type IN
 ('module_room_status', 'module_device_status', 'module_reservation_status', 'module_room_type', 'module_device_type', 'module_yes_no', 'module_review_result');

INSERT INTO sys_dict_type (dict_name, dict_type, status, create_by, create_time, remark) VALUES
 ('实验室状态', 'module_room_status', '0', 'admin', sysdate(), '实验室启用状态'),
 ('设备状态', 'module_device_status', '0', 'admin', sysdate(), '设备启用状态'),
 ('预约状态', 'module_reservation_status', '0', 'admin', sysdate(), '预约流程状态'),
 ('实验室类型', 'module_room_type', '0', 'admin', sysdate(), '实验室分类'),
 ('设备类型', 'module_device_type', '0', 'admin', sysdate(), '设备分类'),
 ('是否', 'module_yes_no', '0', 'admin', sysdate(), '通用是否选项'),
 ('审核结果', 'module_review_result', '0', 'admin', sysdate(), '预约审核结果');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark) VALUES
 (1, '正常', '0', 'module_room_status', '', 'primary', 'Y', '0', 'admin', sysdate(), ''),
 (2, '维护', '1', 'module_room_status', '', 'warning', 'N', '0', 'admin', sysdate(), ''),
 (3, '停用', '2', 'module_room_status', '', 'danger', 'N', '0', 'admin', sysdate(), ''),
 (1, '正常', '0', 'module_device_status', '', 'primary', 'Y', '0', 'admin', sysdate(), ''),
 (2, '维护', '1', 'module_device_status', '', 'warning', 'N', '0', 'admin', sysdate(), ''),
 (3, '停用', '2', 'module_device_status', '', 'danger', 'N', '0', 'admin', sysdate(), ''),
 (1, '待审核', '0', 'module_reservation_status', '', 'warning', 'Y', '0', 'admin', sysdate(), ''),
 (2, '已通过', '1', 'module_reservation_status', '', 'success', 'N', '0', 'admin', sysdate(), ''),
 (3, '已驳回', '2', 'module_reservation_status', '', 'danger', 'N', '0', 'admin', sysdate(), ''),
 (4, '已取消', '3', 'module_reservation_status', '', 'info', 'N', '0', 'admin', sysdate(), ''),
 (5, '已完成', '4', 'module_reservation_status', '', 'success', 'N', '0', 'admin', sysdate(), ''),
 (6, '已签到', '5', 'module_reservation_status', '', 'primary', 'N', '0', 'admin', sysdate(), ''),
 (7, '已爽约', '6', 'module_reservation_status', '', 'danger', 'N', '0', 'admin', sysdate(), ''),
 (1, '化学实验室', 'CHEMISTRY', 'module_room_type', '', 'primary', 'Y', '0', 'admin', sysdate(), ''),
 (2, '生物实验室', 'BIOLOGY', 'module_room_type', '', 'success', 'N', '0', 'admin', sysdate(), ''),
 (3, '材料实验室', 'MATERIAL', 'module_room_type', '', 'warning', 'N', '0', 'admin', sysdate(), ''),
 (4, '电子实验室', 'ELECTRONICS', 'module_room_type', '', 'info', 'N', '0', 'admin', sysdate(), ''),
 (5, '计算机实验室', 'COMPUTER', 'module_room_type', '', 'info', 'N', '0', 'admin', sysdate(), ''),
 (1, '计量分析', 'MEASURE', 'module_device_type', '', 'primary', 'Y', '0', 'admin', sysdate(), ''),
 (2, '分离提纯', 'SEPARATE', 'module_device_type', '', 'success', 'N', '0', 'admin', sysdate(), ''),
 (3, '加热控温', 'HEATING', 'module_device_type', '', 'warning', 'N', '0', 'admin', sysdate(), ''),
 (4, '显微观察', 'MICROSCOPE', 'module_device_type', '', 'info', 'N', '0', 'admin', sysdate(), ''),
 (5, '其他', 'OTHER', 'module_device_type', '', 'info', 'N', '0', 'admin', sysdate(), ''),
 (1, '否', '0', 'module_yes_no', '', 'info', 'Y', '0', 'admin', sysdate(), ''),
 (2, '是', '1', 'module_yes_no', '', 'success', 'N', '0', 'admin', sysdate(), ''),
 (1, '通过', '1', 'module_review_result', '', 'success', 'Y', '0', 'admin', sysdate(), ''),
 (2, '驳回', '2', 'module_review_result', '', 'danger', 'N', '0', 'admin', sysdate(), '');

-- 生成器字段配置：为枚举字段绑定字典
UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.dict_type = 'module_room_type', c.html_type = 'select'
WHERE t.table_name = 'room' AND c.column_name = 'room_type';

UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.dict_type = 'module_room_status', c.html_type = 'radio'
WHERE t.table_name = 'room' AND c.column_name = 'status';

UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.dict_type = 'module_device_type', c.html_type = 'select'
WHERE t.table_name = 'device' AND c.column_name = 'device_type';

UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.dict_type = 'module_device_status', c.html_type = 'radio'
WHERE t.table_name = 'device' AND c.column_name = 'status';

UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.dict_type = 'module_yes_no', c.html_type = 'radio'
WHERE t.table_name = 'device' AND c.column_name = 'requires_training';

UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.dict_type = 'module_reservation_status', c.html_type = 'radio'
WHERE t.table_name = 'reservation' AND c.column_name = 'status';

UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.dict_type = 'module_yes_no', c.html_type = 'radio'
WHERE t.table_name = 'reservation' AND c.column_name = 'review_required';

UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.dict_type = 'module_review_result', c.html_type = 'radio'
WHERE t.table_name = 'reservation_review' AND c.column_name = 'result';

UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.dict_type = 'module_yes_no', c.html_type = 'radio'
WHERE t.table_name = 'safety_exam_record' AND c.column_name = 'passed';

SELECT t.table_name, c.column_name, c.column_comment, c.html_type, c.dict_type
FROM gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
WHERE c.dict_type LIKE 'module_%' ORDER BY t.table_name, c.sort;
