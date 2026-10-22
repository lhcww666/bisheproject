-- 扩展实验室设备档案，保证每个实验室都有多种可选设备。
-- 使用前提：已执行 sql/module_core.sql、sql/module/05-module-seed-data.sql。
-- 可重复执行。
USE bishe_schema;
SET NAMES utf8mb4 COLLATE utf8mb4_general_ci;

SET @room_a101 := (SELECT id FROM room WHERE room_code = 'A101' LIMIT 1);
SET @room_b203 := (SELECT id FROM room WHERE room_code = 'B203' LIMIT 1);
SET @room_c301 := (SELECT id FROM room WHERE room_code = 'C301' LIMIT 1);
SET @room_d102 := (SELECT id FROM room WHERE room_code = 'D102' LIMIT 1);
SET @room_e105 := (SELECT id FROM room WHERE room_code = 'E105' LIMIT 1);

INSERT IGNORE INTO device
(room_id, device_code, device_name, device_type, quantity, status, requires_training, create_by, create_time, remark)
VALUES
(@room_a101, 'EQ-A101-04', '磁力搅拌器', 'OTHER', 4, '0', '0', 'seed', sysdate(), '常规溶液配制与搅拌'),
(@room_a101, 'EQ-A101-05', '台式 pH 计', 'MEASURE', 2, '0', '0', 'seed', sysdate(), '使用后及时清洗电极并浸泡保存'),
(@room_a101, 'EQ-A101-06', '真空干燥箱', 'HEATING', 1, '0', '1', 'seed', sysdate(), '禁止放入易燃溶剂和密闭容器'),
(@room_a101, 'EQ-A101-07', '分液漏斗振荡器', 'SEPARATE', 2, '0', '0', 'seed', sysdate(), '使用前确认分液漏斗固定牢靠'),

(@room_b203, 'EQ-B203-03', 'PCR 扩增仪', 'OTHER', 2, '0', '1', 'seed', sysdate(), '生物实验专用，需完成设备培训'),
(@room_b203, 'EQ-B203-04', '恒温培养箱', 'HEATING', 3, '0', '0', 'seed', sysdate(), '按培养温度登记使用'),
(@room_b203, 'EQ-B203-05', '生物安全柜', 'OTHER', 1, '0', '1', 'seed', sysdate(), '操作前检查气流和视窗位置'),
(@room_b203, 'EQ-B203-06', '核酸电泳仪', 'SEPARATE', 4, '0', '0', 'seed', sysdate(), '电泳槽使用后及时清洗'),

(@room_c301, 'EQ-C301-03', '马弗炉', 'HEATING', 1, '0', '1', 'seed', sysdate(), '高温设备，须佩戴隔热防护并登记'),
(@room_c301, 'EQ-C301-04', '金相显微镜', 'MICROSCOPE', 2, '0', '0', 'seed', sysdate(), '样品表面应清洁平整'),
(@room_c301, 'EQ-C301-05', '超声波清洗机', 'SEPARATE', 2, '0', '0', 'seed', sysdate(), '禁止空槽运行'),
(@room_c301, 'EQ-C301-06', '数显硬度计', 'MEASURE', 1, '0', '1', 'seed', sysdate(), '按材料类型选择测试载荷'),

(@room_d102, 'EQ-D102-03', '函数信号发生器', 'MEASURE', 4, '0', '0', 'seed', sysdate(), '电子电路调试使用'),
(@room_d102, 'EQ-D102-04', '数字万用表', 'MEASURE', 6, '0', '0', 'seed', sysdate(), '测量前确认档位和量程'),
(@room_d102, 'EQ-D102-05', '逻辑分析仪', 'MEASURE', 2, '0', '0', 'seed', sysdate(), '数字信号和总线调试使用'),
(@room_d102, 'EQ-D102-06', '恒温焊接台', 'HEATING', 3, '0', '1', 'seed', sysdate(), '焊接时开启排风并远离可燃物'),

(@room_e105, 'EQ-E105-02', '嵌入式开发板套件', 'OTHER', 20, '0', '0', 'seed', sysdate(), '课程实验常用开发套件'),
(@room_e105, 'EQ-E105-03', '网络测试仪', 'MEASURE', 2, '0', '0', 'seed', sysdate(), '网络布线与连通性测试'),
(@room_e105, 'EQ-E105-04', '计算服务器节点', 'OTHER', 2, '0', '1', 'seed', sysdate(), '须遵守账号和算力使用规范'),
(@room_e105, 'EQ-E105-05', '虚拟仿真实验终端', 'OTHER', 10, '0', '0', 'seed', sysdate(), '虚拟仿真课程使用');

SELECT r.room_code, r.room_name, COUNT(d.id) AS device_count,
       SUM(d.status = '0') AS available_device_count
FROM room r
LEFT JOIN device d ON d.room_id = r.id
GROUP BY r.id, r.room_code, r.room_name
ORDER BY r.room_code;
