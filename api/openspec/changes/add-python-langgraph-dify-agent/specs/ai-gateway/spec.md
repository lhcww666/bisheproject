## ADDED Requirements

### Requirement: 统一的 Agent 入口
系统 MUST 为浏览器 Agent 请求提供统一的 Spring Boot 入口，不因选择 Python 或 Dify 引擎而改变前端调用方式。

#### Scenario: 切换引擎不修改前端
- **WHEN** 管理员切换当前 Agent 引擎
- **THEN** 浏览器继续使用相同的会话和对话接口
- **AND** 响应继续使用统一的事件契约

### Requirement: 用户独占会话
系统 MUST 将每个 Agent 会话绑定到当前登录用户。

#### Scenario: 其他用户访问会话
- **WHEN** 用户请求访问其他用户的会话
- **THEN** 系统拒绝访问
- **AND** 不得返回该会话及其消息内容

### Requirement: 统一流式事件
系统 MUST 通过带类型的事件流返回 Agent 执行进度。

#### Scenario: 回答过程中调用工具
- **WHEN** Agent 在生成回答时调用业务工具
- **THEN** 事件流返回包含状态和追踪 ID 的工具事件
- **AND** 事件流只能以一个完成事件或一个错误事件结束

### Requirement: AI 依赖故障降级
Python 或 Dify 不可用时，系统 MUST 保持普通实验室业务可用。

#### Scenario: Python 服务超时
- **WHEN** Python Agent 服务超过配置的超时时间
- **THEN** Gateway 返回可恢复的 Agent 错误
- **AND** 实验室、预约、审核、安全考试和信用页面仍可正常使用
