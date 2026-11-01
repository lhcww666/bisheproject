# Python AI Agent 接口设计说明书

## 1. 文档定位

本文定义 `py-agent` 分支中浏览器、Spring Boot AI Gateway、FastAPI、LangGraph、Dify 和内部业务工具之间的接口契约。

本文是设计稿。接口实现状态以代码和 OpenSpec 任务清单为准。

## 2. 服务与网络边界

| 服务 | 开发地址 | 可访问范围 |
| --- | --- | --- |
| Vue | `http://127.0.0.1:8090/admin/` | 浏览器用户 |
| Spring Boot | `http://localhost:8087` | 浏览器与服务端组件 |
| FastAPI | `http://127.0.0.1:8091` | Spring Boot、Dify |
| Dify | 由本地 Docker 环境决定 | Spring Boot、FastAPI 运维人员 |

浏览器不得直接调用 FastAPI、Dify 或 `/internal/agent/**` 接口。

## 3. 通用约定

### 3.1 公共接口

- 数据格式：JSON，流式接口使用 SSE。
- 编码：UTF-8。
- 认证：若依 JWT，通过 `Authorization: Bearer <token>` 传递。
- 权限：`module:agent:use`、`module:agent:audit` 等菜单权限。
- 时间：`yyyy-MM-dd HH:mm:ss`。
- 普通响应：若依 `AjaxResult`。
- 请求标识：客户端或网关生成 `requestId`。

普通响应示例：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

### 3.2 内部请求头

Python 和 Dify 调用 Spring Boot 内部工具时必须携带：

| 请求头 | 必需 | 说明 |
| --- | --- | --- |
| `X-Agent-Context` | 是 | Spring Boot 签发的短期上下文令牌 |
| `X-Request-Id` | 是 | 幂等与链路追踪 ID |
| `X-Idempotency-Key` | 写工具必需 | 草稿创建等写操作的幂等键 |
| `Content-Type` | 请求体存在时 | `application/json` |

`X-Agent-Context` 包含用户、角色、会话、trace、有效期和允许的工具集合。Python、Dify 和模型都不能自行覆盖其中的身份字段。

### 3.3 错误结构

普通错误响应：

```json
{
  "code": 403,
  "msg": "当前角色无权调用该工具",
  "data": {
    "errorCode": "AGENT_TOOL_DENIED",
    "requestId": "req-20261101-001",
    "traceId": "trace-20261101-001",
    "retryable": false
  }
}
```

错误码：

| errorCode | HTTP 状态 | 含义 |
| --- | --- | --- |
| `AGENT_SERVICE_UNAVAILABLE` | 503 | Python 或 Dify 服务不可用 |
| `AGENT_TIMEOUT` | 504 | AI 服务或工具调用超时 |
| `AGENT_ENGINE_DISABLED` | 503 | 当前引擎未启用 |
| `AGENT_SESSION_NOT_FOUND` | 404 | 会话不存在 |
| `AGENT_SESSION_FORBIDDEN` | 403 | 会话不属于当前用户 |
| `AGENT_INVALID_CONTEXT` | 401 | 服务上下文无效或过期 |
| `AGENT_TOOL_DENIED` | 403 | 工具不在角色白名单内 |
| `AGENT_INVALID_ARGUMENT` | 400 | 工具参数不合法 |
| `AGENT_KNOWLEDGE_INSUFFICIENT` | 200 | 无足够可靠知识，返回安全拒答 |
| `AGENT_DRAFT_EXPIRED` | 409 | 预约草稿已过期 |
| `AGENT_DRAFT_CONFLICT` | 409 | 草稿状态或业务校验冲突 |
| `AGENT_RATE_LIMITED` | 429 | 请求频率超限 |

## 4. 浏览器访问的 Gateway 接口

基础路径：`/module/agent`

### 4.1 查询 Agent 状态

`GET /module/agent/status`

权限：`module:agent:use`

响应数据：

```json
{
  "enabled": true,
  "engine": "python",
  "pythonConfigured": true,
  "pythonAvailable": false,
  "difyConfigured": false,
  "role": "STUDENT"
}
```

不返回模型密钥、Dify 密钥或内部服务地址。

`pythonAvailable` 表示 Gateway 实际探测 `/api/ai/health` 的结果；`pythonConfigured` 和 `difyConfigured` 只表示服务地址是否配置。

### 4.2 查询会话列表

`GET /module/agent/sessions`

响应数据：

```json
[
  {
    "sessionId": "101",
    "title": "预约化学实验室",
    "engine": "python",
    "createdAt": "2026-11-01 14:00:00",
    "updatedAt": "2026-11-01 14:05:00"
  }
]
```

只返回当前登录用户的会话。

### 4.3 创建会话

`POST /module/agent/sessions`

请求：

```json
{
  "title": "预约咨询",
  "engine": "python"
}
```

字段约束：

- `title` 可省略，由服务端生成。
- `engine` 可省略，使用服务端默认值。
- `engine` 仅允许 `python` 或 `dify`。

响应：

```json
{
  "code": 200,
  "msg": "创建成功",
  "data": {
    "sessionId": "101"
  }
}
```

### 4.4 查询会话消息

`GET /module/agent/sessions/{sessionId}/messages`

请求前必须验证会话归属。响应消息中的 `references` 表示知识引用，`toolCalls` 表示脱敏后的工具摘要。

### 4.5 删除会话

`DELETE /module/agent/sessions/{sessionId}`

仅允许删除当前用户自己的会话。是否物理删除或逻辑删除由持久化实现决定，但删除后不得继续用于发送消息。

### 4.6 同步对话

`POST /module/agent/chat`

用于接口调试或非流式客户端，正式页面优先使用流式接口。

请求：

```json
{
  "sessionId": "101",
  "message": "我下周有哪些预约？",
  "engine": "python"
}
```

响应：

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "messageId": "9001",
    "sessionId": "101",
    "answer": "你下周有 2 条预约……",
    "toolCalls": [],
    "references": [],
    "traceId": "trace-20261101-001"
  }
}
```

### 4.7 流式对话

`POST /module/agent/chat/stream`

响应类型：`text/event-stream`

请求体与同步对话相同。SSE 事件必须包含 `event`，`data` 为 JSON。

#### 开始事件

```text
event: start
data: {"type":"start","sessionId":"101","traceId":"trace-20261101-001"}
```

#### 文本增量

```text
event: message
data: {"type":"message.delta","content":"你下周"}
```

#### 工具状态

```text
event: tool
data: {"type":"tool","toolName":"queryMyReservations","status":"running","toolCallId":"tc-001"}
```

工具完成时使用相同 `toolCallId`，状态为 `succeeded` 或 `failed`，不得返回完整鉴权上下文。

#### 引用

```text
event: citation
data: {"type":"citation","items":[{"documentId":"doc-01","title":"实验室安全守则","score":0.91}]}
```

#### 预约草稿

```text
event: draft
data: {"type":"draft","draftId":"draft-001","status":"PENDING","expiresAt":"2026-11-01 14:30:00"}
```

#### 完成事件

```text
event: done
data: {"type":"done","messageId":"9001","traceId":"trace-20261101-001"}
```

#### 错误事件

```text
event: error
data: {"type":"error","errorCode":"AGENT_TIMEOUT","message":"助手响应超时，请稍后重试","retryable":true}
```

每次流式请求只能出现一个终态事件：`done` 或 `error`。

### 4.8 查询预约草稿

`GET /module/agent/drafts/{draftId}`

只允许草稿所属用户查询。响应包含结构化预约参数、状态、过期时间和风险提示，不包含数据库实体引用。

### 4.9 确认预约草稿

`POST /module/agent/drafts/{draftId}/confirm`

该接口不接受 `userId` 或角色覆盖字段。服务端必须：

1. 验证草稿归属和状态。
2. 验证草稿未过期。
3. 将草稿转换为正式预约请求。
4. 调用原预约 Service 再次执行时间、容量、准入、冲突、设备、考试和信用校验。
5. 成功后把草稿标记为已确认。

响应：

```json
{
  "code": 200,
  "msg": "预约申请已提交",
  "data": {
    "reservationId": "501",
    "status": "PENDING"
  }
}
```

### 4.10 取消预约草稿

`POST /module/agent/drafts/{draftId}/cancel`

只取消临时草稿，不取消已经创建的正式预约。

## 5. Spring Boot 调用 FastAPI 的接口

基础路径：`/api/ai`

Python 服务接收 `X-Agent-Context` 和 `X-Request-Id`。服务端不能信任请求体中的身份字段。

### 5.1 健康检查

`GET /api/ai/health`

响应：

```json
{
  "status": "ok",
  "dependencies": {
    "llm": "ok",
    "difyKnowledge": "ok",
    "javaTools": "ok"
  }
}
```

健康检查不得返回模型名称之外的供应商密钥、内部 token 或完整异常堆栈。

### 5.2 Python 流式对话

`POST /api/ai/chat/stream`

请求：

```json
{
  "sessionId": "101",
  "message": "帮我看看下周三下午能不能预约化学实验室",
  "history": [
    {
      "role": "user",
      "content": "我想做萃取实验"
    }
  ]
}
```

Python 返回与 Gateway 相同语义的 SSE 事件。Spring Boot 可以补充用户展示字段，但不得改变工具执行结果。

### 5.3 预约子 Agent

`POST /api/ai/reservation/analyze`

供 Dify HTTP 节点调用，也用于单独测试 LangGraph 预约流程。

请求：

```json
{
  "sessionId": "101",
  "message": "下周三下午做萃取实验，两个人",
  "state": {
    "entities": {},
    "missingFields": []
  }
}
```

响应：

```json
{
  "code": 200,
  "data": {
    "status": "need_more_info",
    "reply": "请补充需要使用的时间和设备。",
    "missingFields": [
      "startTime",
      "endTime",
      "deviceIds"
    ],
    "draft": null,
    "traceId": "trace-20261101-001"
  }
}
```

`status` 允许值：

| 状态 | 含义 |
| --- | --- |
| `need_more_info` | 仍需用户补充参数 |
| `draft_ready` | 已生成待确认草稿 |
| `ineligible` | 当前用户不满足预约条件 |
| `no_resource` | 没有可用资源 |
| `rejected` | 请求不符合受控业务边界 |

## 6. Spring Boot 内部受控工具

基础路径：`/internal/agent/tools`

这些接口只能从内部网络访问，并要求有效签名上下文。

### 6.1 查询本人预约

`GET /internal/agent/tools/reservations/my`

查询参数：`status`、`pageNum`、`pageSize`。

工具使用上下文中的用户 ID，不接受 `userId` 参数。

### 6.2 查询本人安全考试状态

`GET /internal/agent/tools/safety-exam/status`

只允许学生角色。

### 6.3 查询本人信用

`GET /internal/agent/tools/credit/current`

只允许学生角色。

### 6.4 查询可用实验室

`POST /internal/agent/tools/rooms/available`

请求：

```json
{
  "startTime": "2026-11-08 14:00:00",
  "endTime": "2026-11-08 16:00:00",
  "participantCount": 2,
  "experimentType": "CHEMISTRY",
  "deviceIds": [3]
}
```

Spring Boot 必须复用原预约规则中的开放时间、容量、状态、准入和冲突校验。

### 6.5 查询可用设备

`POST /internal/agent/tools/devices/available`

请求：

```json
{
  "roomId": 12,
  "startTime": "2026-11-08 14:00:00",
  "endTime": "2026-11-08 16:00:00"
}
```

### 6.6 查询预约审核信息

`POST /internal/agent/tools/reservation-review/query`

请求：

```json
{
  "reservationId": 501
}
```

必须按当前用户、教师负责范围或管理员授权范围过滤。

### 6.7 创建预约草稿

`POST /internal/agent/tools/reservation-drafts`

只允许学生角色，必须携带 `X-Idempotency-Key`。

请求：

```json
{
  "sessionId": "101",
  "experimentName": "有机溶剂萃取",
  "roomId": 12,
  "startTime": "2026-11-08 14:00:00",
  "endTime": "2026-11-08 16:00:00",
  "participantCount": 2,
  "deviceIds": [3],
  "remark": "需要使用通风橱"
}
```

响应只返回草稿 ID、结构化参数、状态和风险提示，不直接创建 `reservation`。

## 7. Dify 调用契约

Spring Boot 通过 Java 侧 `DifyWorkflowClient` 调用 Dify Workflow API。Python 侧的 `integrations/dify_api.py` 只负责服务端知识检索，不承担浏览器对话入口。浏览器不接触具体 Dify 路径和密钥。

### 7.1 输入变量

| 变量 | 说明 |
| --- | --- |
| `query` | 用户原始问题 |
| `session_id` | 会话 ID |
| `trace_id` | 链路 ID |
| `role` | 从签名上下文解析的角色 |
| `history` | 必要的历史消息摘要 |
| `agent_context` | 短期签名上下文，仅用于服务端 HTTP 节点，不进入用户可见输出 |

Dify 调用 LangGraph 时，将 `agent_context` 写入 `X-Agent-Context` 服务端 HTTP 请求头。该变量不得映射到用户可见输出、浏览器响应或审计明文日志。

### 7.2 输出结构

```json
{
  "status": "succeeded",
  "answer": "推荐以下实验室……",
  "citations": [],
  "draft": null,
  "traceId": "trace-20261101-001"
}
```

Dify 输出必须经过 Spring Boot 归一化后才能发送给前端。

## 8. 幂等与重试

- 只读工具可以按配置进行有限重试。
- 草稿创建必须使用 `X-Idempotency-Key`。
- 相同幂等键再次请求时返回原草稿，不重复插入。
- 正式预约接口由前端确认动作触发，不以 Dify 的重试作为确认。
- `requestId`、`toolCallId` 和 `traceId` 均写入审计记录。

## 9. 安全要求

1. 公共接口必须验证 JWT 和 `module:agent:*` 权限。
2. 内部工具必须验证签名上下文的签名、有效期、受众和允许工具。
3. `X-Agent-Context` 不记录到普通应用日志。
4. 模型生成的参数必须经过 Pydantic 和 Java Bean Validation 双重校验。
5. 模型输出不能覆盖用户、角色、审批状态和业务状态。
6. 所有写操作必须由用户确认并由 Spring Boot 重新执行业务校验。
7. Dify 与模型密钥不进入源码、浏览器存储或提交记录。

## 10. 兼容与版本管理

- 公共接口基础版本使用 `/module/agent`。
- Python 服务接口使用 `/api/ai`。
- 发生不兼容字段变更时增加版本段，例如 `/api/ai/v2`。
- SSE 事件新增字段必须保持向后兼容。
- Dify 工作流导出文件必须与对应接口字段同时提交。

## 11. 测试要求

每个公共接口至少覆盖：

- 正常请求
- 缺少参数
- 非法会话或草稿 ID
- 未登录
- 无权访问
- 用户之间的数据隔离

每个内部工具至少覆盖：

- 有效上下文
- 过期或伪造上下文
- 角色白名单
- 参数越界
- 超时与异常转换
- 工具审计写入

预约链路还必须覆盖：

- 时间格式和先后顺序错误
- 实验室容量、准入和冲突
- 设备归属和状态
- 安全考试过期
- 信用分不足
- 重复创建草稿
- 草稿过期
- 模型尝试直接确认预约

## 12. 首版验收接口清单

首版只要求以下链路全部通过：

1. `POST /module/agent/sessions`
2. `POST /module/agent/chat/stream`
3. `GET /internal/agent/tools/reservations/my`
4. `GET /internal/agent/tools/safety-exam/status`
5. `GET /internal/agent/tools/credit/current`
6. `POST /api/ai/chat/stream`
7. `GET /api/ai/health`
8. `POST /internal/agent/tools/reservation-drafts`
9. `POST /module/agent/drafts/{draftId}/confirm`

其余接口可以在同一条链路稳定后再实现。

## 13. 当前实现状态

已实现：

| 方法 | 路径 | 状态 |
| --- | --- | --- |
| GET | `/module/agent/status` | 已实现 |
| GET | `/module/agent/sessions` | 已实现 |
| POST | `/module/agent/sessions` | 已实现 |
| GET | `/module/agent/sessions/{sessionId}/messages` | 已实现 |
| DELETE | `/module/agent/sessions/{sessionId}` | 已实现 |

设计完成、尚未实现：

- 同步和 SSE 对话。
- FastAPI 内部接口。
- Dify 工作流调用。
- 预约草稿确认。
- 内部业务工具。
- Agent 审计查询页面。
