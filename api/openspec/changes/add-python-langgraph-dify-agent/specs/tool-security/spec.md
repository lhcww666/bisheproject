## ADDED Requirements

### Requirement: 可信身份上下文
内部 Agent 工具 MUST 从 Spring Boot 签发的短期签名上下文中取得用户身份和角色。

#### Scenario: 模型提供其他用户 ID
- **WHEN** 模型输出或 Python 请求包含其他用户 ID
- **THEN** 工具忽略该值
- **AND** 工具使用签名上下文中的用户身份

### Requirement: 按角色限制工具
系统 MUST 只向当前角色开放其白名单中的工具。

#### Scenario: 学生调用管理员工具
- **WHEN** 学生尝试调用教师或管理员工具
- **THEN** 工具接口拒绝调用
- **AND** 记录一条拒绝访问的审计事件

### Requirement: Agent 只能创建草稿
Agent 唯一可用的写操作 MUST 是创建临时预约草稿。

#### Scenario: 模型尝试直接确认预约
- **WHEN** 模型输出要求确认预约
- **THEN** 系统不得创建正式预约
- **AND** 确认必须由浏览器显式发起，并经过 Java 业务校验

### Requirement: 可审计的工具执行
系统 MUST 记录工具名称、请求 ID、会话 ID、用户 ID、执行状态、耗时和脱敏后的参数摘要。

#### Scenario: 工具执行失败
- **WHEN** 内部工具返回错误或超时
- **THEN** 系统使用追踪 ID 持久化失败记录
- **AND** 不得保存密钥和完整认证请求头

### Requirement: 可幂等重试
Agent 发起的工具请求 MUST 支持幂等键。

#### Scenario: Dify 重试草稿创建
- **WHEN** 相同请求 ID 被重复提交
- **THEN** 系统返回已有草稿
- **AND** 不得创建重复草稿
