## ADDED Requirements

### Requirement: 只读业务查询
Python Agent MUST 通过调用 Spring Boot 内部工具完成授权范围内的实验室查询，不得直接访问 MySQL。

#### Scenario: 学生查询本人预约
- **WHEN** 学生要求查看自己的预约记录
- **THEN** Agent 调用当前用户预约工具
- **AND** 回答使用工具返回的真实业务数据

### Requirement: 有状态预约信息收集
LangGraph Agent MUST 保存足够的会话状态，以便跨轮收集缺失的预约信息。

#### Scenario: 缺少预约时间
- **WHEN** 学生提出预约请求但没有指定时间
- **THEN** Agent 要求补充预约时间
- **AND** 同一会话的下一轮请求继续预约流程，而不是从头开始

### Requirement: LangChain 组件边界
Python 服务 MUST 使用 LangChain 完成模型调用、工具参数、提示词组合和解析，并由 LangGraph 负责工作流状态流转。

#### Scenario: 模型选择业务工具
- **WHEN** Agent 需要查询实验室数据
- **THEN** 模型输出被解析为已注册的 LangChain 工具调用
- **AND** LangGraph 工具节点校验后通过 Java API 客户端执行

### Requirement: 有边界的 Python 服务接口
Python 服务 MUST 提供版本化的健康检查、对话、流式对话和预约分析接口。

#### Scenario: 查询服务健康状态
- **WHEN** Spring Boot 调用 Python 健康检查接口
- **THEN** 服务返回就绪状态
- **AND** 响应不得暴露任何密钥
