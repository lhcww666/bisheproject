# SQL 一键初始化发布包

本目录保存可交付的 MySQL 完整初始化文件。主文件：

```text
01-bishe_schema_full.sql
```

## 用途

在一台新设备上安装好 MySQL 8 后，只需导入主文件即可得到完整数据库，包括：

1. 若依基础表、系统菜单、管理员账号和基础字典。
2. Magic API 所需的 `magic_api_file`、`magic_backup_record` 表。
3. 中国行政区划 `biz_region` 表及基础数据。
4. 实验室、设备、预约、审核、安全考试和信用分核心业务表。
5. 当前业务字典、菜单、按钮、学生/教师/管理员角色授权。
6. 班级、演示学生、演示教师、实验室准入和班级负责人数据。
7. 预约生命周期、信用违规、安全考试、数据看板和耗材库存。
8. Spring AI 会话、消息、预约草稿、工具审计、知识库、平台帮助和评测数据。

目标数据库名固定为：

```text
bishe_schema
```

完整文件会自动创建数据库并切换数据库，无需提前手工建库。

## 导入方式

Linux/macOS：

```bash
mysql -u root -p --default-character-set=utf8mb4 < 01-bishe_schema_full.sql
```

Windows PowerShell：

```powershell
cmd /c "mysql -u root -p --default-character-set=utf8mb4 < sql\release\01-bishe_schema_full.sql"
```

也可以在 Navicat、DataGrip 或 IDEA 数据库工具中打开完整文件后整体执行。

## 重要提示

- 该文件是初始化脚本，不是无损迁移脚本。
- 若依基础表和业务表会被删除并重新创建。
- 已有正式数据时不要直接执行，应先备份数据库或改用 `sql/module/` 下的增量脚本。
- 文件包含存储过程定义及 `DELIMITER`，推荐使用 `mysql` 命令行或支持 MySQL 分隔符的数据库客户端。
- 导入完成后检查 `sys_user`、`room`、`reservation`、`knowledge_document` 和 `agent_eval_case` 是否有数据。

## 验证查询

```sql
USE bishe_schema;

SELECT COUNT(*) AS room_count FROM room;
SELECT COUNT(*) AS device_count FROM device;
SELECT COUNT(*) AS user_count FROM sys_user;
SELECT COUNT(*) AS menu_count FROM sys_menu;
SELECT COUNT(*) AS knowledge_document_count FROM knowledge_document;
SELECT COUNT(*) AS agent_eval_case_count FROM agent_eval_case;
```

## 包含范围

完整文件由以下内容按依赖顺序生成：

1. `api/sql/ruoyi-mysql.sql`
2. `api/sql/magic-api-mysql.sql`
3. `api/sql/region-mysql.sql`
4. `sql/module_core.sql`
5. 字段、审计字段、字典、生成器配置和基础演示数据脚本
6. 7 个业务菜单脚本
7. 模块目录、业务按钮、角色、准入、班级、审核和页面结构脚本
8. `20` 至 `42` 号生命周期、业务扩展和 Agent 脚本

旧 `lab_ai_platform.sql`、`lab_ai_platform_core.sql` 以及 PostgreSQL、GaussDB、HighGo 初始化脚本已经从 `springai` 分支移除，避免误执行。`api/sql` 中只保留 MySQL 源脚本，供发布文件重新生成。

## 重新生成

源 SQL 修改后，在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\build-release-sql.ps1
```

脚本会重新生成完整 SQL 和 `SHA256SUMS.txt`。不要手工编辑 `01-bishe_schema_full.sql`。
