# Python AI Agent 服务架构设计说明书

## 1. 文档定位

本文描述 `py-agent` 分支拟采用的独立 AI 应用架构。该分支以 `dev` 的实验室业务为基线，使用 FastAPI、LangChain、LangGraph、Dify 和 Spring Boot AI Gateway 实现智能助手。

本文是设计稿，不代表相应代码已经完成。实施范围和验收契约以 `api/openspec/changes/add-python-langgraph-dify-agent/` 为准。

本文不替代以下文档：

- 系统总体设计：`docs/02-系统概要设计说明书.md`
- 基础业务接口：`docs/03-接口设计说明书.md`
- Python Agent 接口：`docs/13-AI-Agent接口设计说明书.md`

## 2. 建设目标

1. 在现有实验室预约、审核、考试、信用和统计业务之上增加自然语言助手。
2. 使用独立 Python 服务承载模型调用、RAG 和复杂 Agent 编排。
3. 使用 Dify 承载可配置的外层工作流和安全知识库。
4. 保持 Spring Boot 对业务数据、权限和事务的最终控制。
5. 让每次模型运行、工具调用和预约草稿都可以追踪。
6. 保持 AI 服务故障时原有业务系统仍可使用。

## 3. 范围与边界

### 3.1 第一阶段范围

- 会话与消息持久化
- SSE 流式回答
- 本人预约、考试状态、信用和审核结果查询
- 指定时段可用实验室和设备查询
- 安全知识检索与引用
- 多轮补全预约参数
- 预约草稿生成和页面确认
- Python 与 Dify 两种引擎路由
- 工具调用审计和评测

### 3.2 第一阶段不做

- 模型直接访问业务数据库
- 模型直接确认预约、审核通过、扣信用或调整库存
- 不受限制的多 Agent 自由协作
- 同时维护两套权威知识库
- 模型绕过用户确认执行写操作
- 在初始阶段拆分微服务或引入 Kubernetes

## 4. 总体架构

```text
┌──────────────────────────────────────────┐
│ Vue 3 智能助手页面                        │
│ 会话、消息、引用、工具状态、预约草稿确认  │
└───────────────────┬──────────────────────┘
                    │ JWT + SSE
┌───────────────────▼──────────────────────┐
│ Spring Boot AI Gateway                    │
│ 登录用户、角色、会话、审计、引擎路由       │
│ 短期上下文令牌、预约草稿、业务工具         │
└───────────┬──────────────────┬───────────┘
            │                  │
            │ Python 引擎       │ Dify 引擎
            ▼                  ▼
┌─────────────────────┐  ┌──────────────────┐
│ FastAPI             │  │ Dify Workflow     │
│ LangGraph           │◄─┤ HTTP 节点         │
│ LangChain 组件      │  │ 外层流程与知识库   │
└──────────┬──────────┘  └────────┬─────────┘
           │                      │
           └──────────┬───────────┘
                      ▼
          Spring Boot 内部受控工具
          查询、资格检查、草稿创建、审计
                      │
                      ▼
                 MySQL / Redis
```

浏览器只访问 Spring Boot。Python 服务和 Dify 只通过服务端网络访问内部工具接口。

## 5. 部署单元与职责

| 部署单元 | 主要职责 | 数据所有权 |
| --- | --- | --- |
| Vue 前端 | 会话交互、SSE 渲染、引用与草稿确认 | 无业务数据 |
| Spring Boot | 认证授权、业务规则、会话、审计、工具和最终写入 | MySQL、Redis |
| FastAPI | Python AI 服务入口、Pydantic 校验、流式输出 | 仅短期执行状态 |
| LangGraph | 状态图、条件分支、多轮补全和工具循环 | Redis 可选检查点 |
| LangChain | 模型适配、工具定义、Prompt、解析和 RAG 组件 | 无持久化主数据 |
| Dify | 外层工作流、提示词配置和首版权威知识库 | Dify 自身数据库 |
| MySQL | 若依权限和实验室业务主数据 | 业务权威数据 |
| Redis | 登录态、限流、短期 Agent 状态 | 可过期数据 |

## 6. 技术组件分工

### 6.1 FastAPI

- 提供 `/api/ai` 下的服务接口。
- 使用 Pydantic 校验请求和响应。
- 提供同步响应和 SSE 流式响应。
- 将模型、LangGraph 和工具异常转换为统一错误。
- 不保存实验室业务主数据。

### 6.2 LangChain

- 封装 DeepSeek 或其他 OpenAI 兼容模型。
- 定义模型可见的工具名称、描述和参数结构。
- 生成结构化输出并解析工具调用。
- 在扩展阶段提供文档加载、切分和向量检索组件。
- 不负责决定最终业务权限。

### 6.3 LangGraph

- 识别用户意图并选择后续节点。
- 在预约参数不完整时进入追问状态。
- 根据工具结果决定继续查询、生成草稿或解释失败。
- 通过 `sessionId` 恢复多轮状态。
- 在需要用户确认时结束当前图运行，等待下一次前端请求。

### 6.4 Dify

- 提供安全问答和预约分析两个首版工作流。
- 保存经审核的安全知识文档。
- 通过 HTTP 节点调用 LangGraph 复杂子流程。
- 使用服务端密钥访问，不向浏览器暴露工作流凭证。

### 6.5 Spring Boot AI Gateway

- 从 JWT 解析用户身份，不信任模型传入的身份字段。
- 根据配置选择 Python 或 Dify 引擎。
- 通过 Java 侧 `DifyWorkflowClient` 调用 Dify Workflow API。
- 统一处理流式事件和错误。
- 生成短期签名上下文供内部服务使用。
- 记录会话、消息、运行、工具调用和草稿。
- 提供受控业务工具。

## 7. 核心请求链路

### 7.1 Python 直连路径

```text
POST /module/agent/chat/stream
  -> Spring Boot 校验 JWT 和 module:agent:use
  -> 创建 traceId 和签名 Agent 上下文
  -> FastAPI /api/ai/chat/stream
  -> LangGraph 识别意图
  -> LangChain 选择工具
  -> Spring Boot 内部工具
  -> 业务 Service 执行权限与规则校验
  -> SSE 返回工具状态、引用、消息和完成事件
  -> Spring Boot 保存消息与审计
```

### 7.2 Dify 外层路径

```text
POST /module/agent/chat/stream
  -> Spring Boot 检查用户权限
  -> 调用 Dify Workflow API
  -> Dify 完成意图路由和简单参数检查
  -> Dify HTTP 节点调用 LangGraph 预约子 Agent
  -> LangGraph 调用 Spring Boot 受控工具
  -> Dify 组织工作流输出
  -> Spring Boot 归一化为 SSE
  -> 浏览器展示答案或预约草稿
```

### 7.3 预约确认路径

```text
Agent 创建预约草稿
  -> 前端展示时间、实验室、设备、人数和风险提示
  -> 用户点击确认
  -> Spring Boot 校验草稿归属与有效期
  -> 调用原预约 Service 重新执行全部事务规则
  -> 创建正式预约
  -> 记录确认操作和工具调用关联
```

模型不能调用“确认预约”工具。确认动作必须来自浏览器用户。

## 8. Python 工程结构

目录风格参考现有 FastAPI 项目 `news_fastapi_project` 的扁平分层，并替换业务相关目录：

```text
ai/
├── main.py
├── pyproject.toml
├── uv.lock
├── test_main.http
├── .env.example
├── Dockerfile
├── config/
│   ├── settings.py
│   ├── llm_conf.py
│   ├── dify_conf.py
│   └── java_conf.py
├── routers/
│   ├── chat.py
│   ├── agent.py
│   ├── knowledge.py
│   ├── reservation.py
│   └── health.py
├── schemas/
│   ├── base.py
│   ├── chat.py
│   ├── agent.py
│   ├── knowledge.py
│   └── reservation.py
├── services/
│   ├── chat_service.py
│   ├── agent_service.py
│   ├── knowledge_service.py
│   └── reservation_service.py
├── agent/
│   ├── state.py
│   ├── graph.py
│   ├── nodes.py
│   └── prompts.py
├── tools/
│   ├── business_tools.py
│   ├── knowledge_tools.py
│   └── registry.py
├── integrations/
│   ├── java_api.py
│   ├── llm.py
│   └── dify_api.py
├── knowledge/
│   ├── loader.py
│   ├── splitter.py
│   └── retriever.py
├── utils/
│   ├── auth.py
│   ├── exception.py
│   ├── exception_handlers.py
│   ├── logging.py
│   └── response.py
├── dify/workflows/
├── deploy/
└── tests/
```

Python 服务不使用 `crud` 和业务 `models` 目录，因为实验室业务数据由 Spring Boot 负责。

## 9. LangGraph 状态与流程

首版状态对象：

| 字段 | 说明 |
| --- | --- |
| `messages` | 当前会话消息 |
| `user_context` | 用户、角色、权限范围、会话与 trace |
| `intent` | 知识、查询、预约或未知 |
| `entities` | 日期、时间、实验室、设备、人数和实验类型 |
| `missing_fields` | 仍需补充的参数 |
| `tool_results` | 受控工具返回值 |
| `retrieved_docs` | 知识片段与来源 |
| `risk_flags` | 越权、资格不足、资源冲突等 |
| `draft` | 待用户确认的预约草稿 |
| `final_answer` | 最终回答 |
| `trace_id` | 全链路追踪标识 |

首版状态图：

```text
normalize_input
      |
classify_intent
      |-- knowledge --> retrieve_knowledge --> generate_answer
      |-- query ------> select_tool --------> execute_tool --> generate_answer
      |-- reservation
      |       |
      |   collect_fields
      |       |-- 缺参数 --> ask_user
      |       `-- 完整 ----> validate_eligibility
      |                         |
      |                    create_draft
      `-- unknown -------> helper_response
```

Redis 只保存可过期的会话工作状态。MySQL 中的 `chat_message` 是用户看到的历史消息权威来源。

## 10. 工具设计

| 工具 | 角色范围 | 写操作 | 最终控制者 |
| --- | --- | --- | --- |
| `queryMyReservations` | 学生 | 否 | Spring Boot |
| `checkSafetyExamStatus` | 学生 | 否 | Spring Boot |
| `queryMyCredit` | 学生 | 否 | Spring Boot |
| `queryAvailableRooms` | 全部已登录角色 | 否 | Spring Boot |
| `queryAvailableDevices` | 全部已登录角色 | 否 | Spring Boot |
| `queryReservationReview` | 按数据范围 | 否 | Spring Boot |
| `searchSafetyKnowledge` | 按知识可见范围 | 否 | Dify/知识服务 |
| `createReservationDraft` | 学生 | 仅临时草稿 | Spring Boot |

工具必须使用当前请求的签名上下文，不接受模型提供的 `userId` 或角色覆盖值。

## 11. 数据设计

| 表 | 关键内容 |
| --- | --- |
| `agent_chat_session` | 用户、标题、引擎、创建和更新时间 |
| `agent_chat_message` | 会话、角色、内容、引用和 trace |
| `agent_run` | 引擎、意图、状态、耗时和模型信息 |
| `agent_tool_call_log` | 工具、脱敏参数、状态、耗时和关联消息 |
| `agent_reservation_draft` | 用户、会话、结构化预约参数、状态和过期时间 |

约束原则：

- 所有会话和草稿必须绑定登录用户。
- 工具日志不得保存密码、JWT、模型密钥和完整的服务上下文。
- 预约草稿不能替代正式预约记录。
- 最终预约写入继续复用原业务 Service 的事务校验。

## 12. 知识库与 RAG

第一版权威知识源为 Dify Knowledge：

```text
审核后的安全文档
  -> Dify 知识库导入与切分
  -> 服务端检索 API
  -> 相关性阈值过滤
  -> 模型根据片段生成回答
  -> 返回标题、片段和引用信息
```

回答约束：

- 引用内容不得超出检索片段支持的范围。
- 无可靠结果时明确提示未找到依据。
- 用户无权访问的文档不得进入检索结果。
- 知识文档更新需要管理员审核。

扩展阶段可以增加 BGE 中文 Embedding、Qdrant 或 pgvector，但同一时间只保留一个权威知识源。

## 13. 安全设计

| 风险 | 控制措施 |
| --- | --- |
| 模型伪造用户身份 | 身份只来自 JWT 和 Spring Boot 签名上下文 |
| 模型越权调用工具 | 按角色和工具白名单控制 |
| 模型直接写业务数据 | Python 无数据库凭据；写工具仅创建草稿 |
| Dify 凭证泄露 | 仅服务端配置，不发送到浏览器 |
| 重复执行 | 请求 ID 和幂等键 |
| Prompt Injection | 提示词不具备授权能力，工具端再次校验 |
| 服务链路拖垮主系统 | 独立超时、熔断式降级和全局限流 |
| 审计泄露敏感信息 | 日志白名单和字段脱敏 |

## 14. 部署结构

开发环境建议：

| 服务 | 本地地址 |
| --- | --- |
| Vue | `http://127.0.0.1:8090/admin/` |
| Spring Boot | `http://localhost:8087` |
| FastAPI | `http://127.0.0.1:8091` |
| MySQL | `localhost:3306` |
| Redis | `localhost:6379` |

Dify 使用独立的 Docker 部署，由环境变量提供模型和服务密钥。仓库只保存工作流导出文件与无密钥的配置示例。

生产演示时，Nginx 只暴露 Vue 和 Spring Boot 公共接口；FastAPI、Dify 和内部工具接口放在服务端内部网络。

## 15. 可观测与故障处理

每次请求需要贯穿以下标识：

```text
requestId -> sessionId -> traceId -> toolCallId
```

必须记录：

- 当前引擎与路由结果
- LangGraph 意图和节点状态
- 工具名称、状态和耗时
- 知识检索数量和引用
- 模型耗时和错误类型
- SSE 开始、结束和异常

失败策略：

- Python 超时：返回可重试错误，不创建业务数据。
- Dify 超时：按配置切换到 Python 路径或直接降级。
- 单个工具失败：向用户说明失败原因，不让模型编造结果。
- 草稿重复提交：按幂等键返回已有草稿。
- 服务鉴权失败：拒绝调用并记录安全审计。

## 16. 评测设计

使用与 `springai` 基线一致的测试口径，至少覆盖：

- 安全知识问答与无依据拒答
- 本人预约、考试和信用查询
- 可用实验室和设备查询
- 时间、人数和设备参数提取
- 多轮补全预约信息
- 资格不足和资源冲突解释
- 越权查询
- 重复 Dify 调用
- 服务超时与降级

核心指标：

| 指标 | 说明 |
| --- | --- |
| 意图识别准确率 | 分类结果是否符合期望 |
| 参数提取准确率 | 时间、人数、设备和实验类型是否正确 |
| 工具选择正确率 | 是否选择预期受控工具 |
| 端到端成功率 | 是否完成预期业务流程 |
| 越权拦截率 | 越权场景是否全部拒绝 |
| 响应时间 | 模型、检索、工具和总链路耗时 |
| 调用成本 | 模型 token 和平台调用成本 |

## 17. 实施顺序

1. Spring Boot AI Gateway 与数据库迁移。
2. FastAPI 基础服务和只读工具。
3. LangGraph 查询与多轮预约收集。
4. Dify 安全问答和预约分析工作流。
5. 知识引用与无依据拒答。
6. 预约草稿确认闭环。
7. SSE 前端、审计和评测页面。
8. 与 Spring AI 基线进行同一测试集比较。

## 18. 风险与后续演进

主要风险是链路长、状态分散和知识库重复。因此首版坚持“Java 控业务、Python 控 Agent、Dify 控外层流程、单一知识源”的边界。

后续可以演进为：

- 引入 MCP 统一工具协议
- 使用 Qdrant 或 pgvector 做 Python RAG 对比
- 增加 Langfuse 等调用链观测
- 增加 Dify 管理端工作流版本控制
- 在资源允许时接入本地 Qwen 模型

这些扩展必须在首条端到端链路稳定后再进行。

## 19. 当前实现进度

截至本次基础网关提交，已完成：

- `agent_chat_session`、`agent_chat_message`、`agent_run`、`agent_tool_call_log`、`agent_reservation_draft` 五张表。
- Spring Boot Agent 会话创建、列表、历史消息和本人删除。
- 会话归属校验与跨用户访问拒绝。
- Agent 状态接口和 Python `/api/ai/health` 探测。
- `module:agent:use` 与 `module:agent:audit` 菜单权限脚本。
- 会话 Service 单元测试。
- HMAC-SHA256 短期 Agent 上下文签发、校验、过期和篡改测试。
- FastAPI 服务骨架、统一异常处理与 `/api/ai/health` 健康接口。
- FastAPI 校验 Java 签发的 `X-Agent-Context`。
- LangChain DeepSeek/OpenAI 兼容模型适配和同步 `/api/ai/chat`。
- Spring Boot `/module/agent/chat` 同步转发、历史消息和双消息保存。
- LangGraph 按业务查询、预约、安全知识和普通对话四类意图路由。
- Java 内部预约查询工具按签名上下文中的用户身份返回本人预约摘要。
- LangChain `queryMyReservations` 工具和 LangGraph 业务查询节点。
- 工具成功、失败与耗时写入 `agent_tool_call_log`。
- Java 内部安全考试和信用工具，以及对应 LangChain 工具封装。
- 业务查询节点按预约、考试、信用和预约资格选择工具。
- Java 可用实验室/设备查询工具和对应 LangChain 工具封装。
- 预约时间、人数、实验和设备需求的结构化抽取。
- 结合历史消息补全预约参数，参数完整后调用可用实验室工具。
- Java 预约草稿创建、归属校验、幂等、过期、查询和取消。
- 草稿确认调用原预约 Service 重新执行业务校验，重复确认不重复写入。
- Python `createReservationDraft` LangChain 工具。
- 智能助手前端会话栏、消息区、同步聊天和意图展示。
- 服务未连接、请求失败和手动刷新状态。
- 同步对话中展示工具名称及成功、失败状态。
- FastAPI `start/tool/message.delta/done/error` SSE 事件。
- Java Gateway 异步代理 SSE，并在完成后保存助手消息。
- 前端 fetch 流解析、消息增量更新和工具状态即时追加。
- 用户明确选择可用实验室后，LangGraph 匹配设备和房间并创建待确认草稿。
- 前端预约草稿确认弹窗、取消和正式提交。
- Java Dify Workflow blocking API 客户端、服务端密钥和 Agent 上下文传递。
- 安全问答和预约分析两份可导入 Dify DSL。
- Java 同步对话支持 `engine=dify`，按安全/预约场景选择对应工作流密钥。
- Python 结构化预约分析接口和多状态 Dify 预约分支工作流。
- 前端新建会话可选择 Python 或 Dify，Python 使用 SSE，Dify 使用同步 Workflow。

尚未完成：

- Dify 工作流实际导入、安全知识库绑定和运行联调。
- Agent 知识引用展示。
