# Change: 优化智能助手的角色表达与回答质量

## Why

当前学生、教师和管理员共用包含技术术语的行为规则，平台使用问题会被误判为越界，预约复合问题也缺少稳定的多轮条件收集。普通用户还会看到模型配置、前后端校验和原始工具调用等维护信息。

## What Changes

- 按角色隔离表达方式，学生和教师使用业务语言，仅管理员展示模型与系统维护信息。
- 将“知识库、设备档案、预约流程”等平台使用说明纳入受角色可见性控制的 RAG 知识。
- 为预约问题增加条件收集规则，支持实验室与设备复合查询，并减少重复追问。
- 禁止模型直接确认预约草稿，最终提交只能由用户在业务页面操作。
- 增加角色语气、平台帮助、复合意图和写操作边界评测用例。

## Impact

- Affected specs: `agent-assistant`
- Affected code: `api/ruoyi-biz/**/agent`、`api/ruoyi-biz/src/main/resources/mapper/module`、`ui/src/views/module/agent`、`sql/module`
- Data migration: 新增 Agent 体验优化 SQL 脚本，扩展现有知识文档类型与可见范围
