-- 代码生成器表配置：模块名、包路径、业务名、类名统一为《命名规范-最终版》的口径
-- 使用前提：已通过"代码生成 -> 导入表"把 7 张业务表导入 gen_table
-- 执行顺序：导入表 -> 本脚本 -> sql/module/01-module-dict.sql -> 生成代码
USE bishe_schema;

UPDATE gen_table SET module_name='module', package_name='com.ruoyi.module.room', class_name='Room', business_name='room', function_name='实验室信息', function_author='itjialu', tpl_web_type='element-plus', gen_path='/' WHERE table_name='room';
UPDATE gen_table SET module_name='module', package_name='com.ruoyi.module.device', class_name='Device', business_name='device', function_name='设备信息', function_author='itjialu', tpl_web_type='element-plus', gen_path='/' WHERE table_name='device';
UPDATE gen_table SET module_name='module', package_name='com.ruoyi.module.reservation', class_name='Reservation', business_name='reservation', function_name='实验室预约', function_author='itjialu', tpl_web_type='element-plus', gen_path='/' WHERE table_name='reservation';
UPDATE gen_table SET module_name='module', package_name='com.ruoyi.module.reservation', class_name='ReservationDevice', business_name='reservationDevice', function_name='预约设备关联', function_author='itjialu', tpl_web_type='element-plus', gen_path='/' WHERE table_name='reservation_device';
UPDATE gen_table SET module_name='module', package_name='com.ruoyi.module.review', class_name='ReservationReview', business_name='reservationReview', function_name='预约审核', function_author='itjialu', tpl_web_type='element-plus', gen_path='/' WHERE table_name='reservation_review';
UPDATE gen_table SET module_name='module', package_name='com.ruoyi.module.safety', class_name='SafetyExamRecord', business_name='safetyExamRecord', function_name='安全考试记录', function_author='itjialu', tpl_web_type='element-plus', gen_path='/' WHERE table_name='safety_exam_record';
UPDATE gen_table SET module_name='module', package_name='com.ruoyi.module.credit', class_name='CreditRecord', business_name='creditRecord', function_name='信用分流水', function_author='itjialu', tpl_web_type='element-plus', gen_path='/' WHERE table_name='credit_record';

-- 开放时间字段：MySQL 的 TIME 映射为 String（HH:mm:ss 文本），避免实体把 "08:00:00" 当 Date 解析
UPDATE gen_table_column c JOIN gen_table t ON c.table_id = t.table_id
SET c.java_type = 'String', c.html_type = 'input'
WHERE t.table_name = 'room' AND c.column_name IN ('open_start_time', 'open_end_time');

SELECT table_name, package_name, business_name, class_name, function_name FROM gen_table ORDER BY table_name;
