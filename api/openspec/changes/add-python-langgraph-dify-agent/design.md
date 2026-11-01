## 背景

`py-agent` 基于 `dev` 中的核心实验室业务。Spring Boot 负责 JWT 认证、角色与数据范围校验、预约规则、审核状态流转、安全考试有效期、信用分阈值和事务写入。新增 AI 组件不得复制或绕过这些规则。

所选技术栈分别承担以下职责：

- FastAPI 提供 Python 服务边界、参数校验、流式输出和服务生命周期管理。
- LangChain 提供模型适配、工具定义、提示词组件、输出解析和可选的知识处理组件。
- LangGraph 提供有状态 Agent 编排、条件路由、工具循环和可恢复的多轮信息收集。
- Dify 提供服务端可视化工作流层，以及知识库和流程配置界面。

## 目标与非目标

- 目标：
  - 不论选择哪种 AI 引擎，浏览器都只访问一个 Spring Boot 入口。
  - 同时支持 Python 直连路径和“Dify 外层、LangGraph 内层”的组合路径。
  - 所有业务写入仍由 Java Service 执行原有事务校验。
  - 每次 Agent 运行和工具调用都可追踪。
  - AI 依赖故障时，原有业务系统仍可使用。
- 非目标：
  - 允许模型或 Dify 直接访问 MySQL 业务表。
  - 允许模型审核预约、修改信用、调整库存或确认预约。
  - 首版支持不受限制的多 Agent 自由协作。
  - 首版同时维护两套权威知识库。
  - 替换 `springai` 分支的实现。

## 关键决策

### 决策：Spring Boot 继续作为业务控制面

浏览器只使用现有 JWT 登录。Spring Boot 解析当前用户和角色，执行接口权限校验，创建 Agent 请求上下文，并且只暴露范围明确的内部工具。

考虑过让 Python 携带浏览器 Token 调用公共用户接口，但没有采用。该方案会增加 Token 暴露范围，使 Python 与所有前端接口结构强耦合，也更难审计授权过程。

### 决策：使用短期签名服务上下文

Spring Boot 为每次 Agent 请求生成短期签名上下文，包含 `userId`、角色、会话 ID、追踪 ID、过期时间和允许调用的工具集合。Python 和 Dify 只能将该上下文传给内部工具接口，不能伪造或修改用户身份。

签名密钥通过被 Git 忽略的本地配置或环境变量提供。内部工具接口不得由公共反向代理暴露。

### 决策：两种引擎使用同一套公共 AI 接口

Spring Boot AI Gateway 统一处理 `python` 和 `dify` 引擎的请求与响应。两种路径都产生相同的消息、工具、引用、草稿、完成和错误事件。

考虑过让前端直接调用不同引擎，但没有采用。该方案会使会话、审计、权限和预约草稿分散在客户端与多个平台中。

### 决策：LangChain 是 LangGraph 内部的组件层

LangChain 负责模型适配、工具参数结构、提示词组合、输出解析以及后续 RAG 组件。LangGraph 负责状态流转和多轮行为，不再单独实现一套只使用 LangChain 的 Agent。

### 决策：Dify 作为外层工作流

选择 Dify 路径时，Spring Boot 调用 Dify Workflow API。Dify 完成意图路由和简单参数检查，再通过 HTTP 节点调用 LangGraph 预约子 Agent。Dify 不持有数据库凭据，也不能直接执行业务写入。

### 决策：首版只使用一个权威知识源

首版使用 Dify 知识库作为经过审核的安全知识权威来源。Python 通过服务端检索工具获取知识。后续可以增加 BGE 和 Qdrant 检索实现用于对比，但同一批文档不能同时由 Dify 知识库和 Python 向量库作为权威来源。

## 总体架构

```text
Vue Agent 页面
    |
    | JWT / SSE
    v
Spring Boot AI Gateway
    |-- 会话与消息持久化
    |-- 引擎路由
    |-- 签名 Agent 上下文
    |-- 审计与草稿接口
    |
    +--> FastAPI + LangGraph + LangChain
    |
    +--> Dify Workflow --> LangGraph 子 Agent
    |
    +--> 内部受控工具 --> 业务 Service --> MySQL
```

## Python 服务结构

Python 项目沿用现有 FastAPI 参考项目的扁平结构：

```text
ai/
├── main.py
├── config/
├── routers/
├── schemas/
├── services/
├── agent/
├── tools/
├── integrations/
├── knowledge/
├── utils/
├── dify/workflows/
├── deploy/
└── tests/
```

`routers` 保存 HTTP 边界，`services` 保存用例，`agent` 保存 LangGraph 状态和图，`tools` 保存模型可调用的工具，`integrations` 保存 Java、模型和 Dify 客户端。Python 服务不包含业务数据库模型和 SQL。

## Agent 状态

第一版 LangGraph 状态包含：

- `messages`
- `user_context`
- `intent`
- `entities`
- `missing_fields`
- `tool_results`
- `retrieved_docs`
- `risk_flags`
- `draft`
- `final_answer`
- `trace_id`

第一版状态图支持知识问答、只读业务查询和预约参数收集。Redis 可以保存以会话 ID 为键的短期检查点，MySQL 仍然是用户可见消息的持久化权威来源。

## 数据所有权

- Spring Boot 和 MySQL 拥有 `agent_chat_session`、`agent_chat_message`、`agent_run`、`agent_tool_call_log`、`agent_reservation_draft` 和全部实验室业务数据。
- Python 只拥有临时执行状态以及可选的 Redis 检查点。
- Dify 拥有首版工作流定义和权威知识文档。
- Dify 工作流导出文件统一存放在 `ai/dify/workflows/` 下，运行期数据库卷不提交到仓库。

## 安全与故障处理

- 工具调用必须携带有效签名上下文，并通过角色白名单。
- 只有 `createReservationDraft` 可以创建临时记录。预约确认仍然是面向用户的 Spring Boot 操作。
- 工具重试使用由请求 ID 和会话 ID 形成的幂等键。
- 模型提示词和用户文本都不能授予业务权限。
- Java 到 Python、Java 到 Dify、Python 到工具的调用都必须设置超时上限。
- Python 或 Dify 不可用时，Gateway 返回可恢复错误，同时保持原有业务页面可用。
- 密钥、Token 和完整的内部工具请求不得进入普通日志。

## 接口范围

详细契约见 `docs/13-AI-Agent接口设计说明书.md`。第一版实现包括：

- 面向浏览器的会话、历史消息、流式对话、状态和草稿确认接口。
- FastAPI 的对话、预约分析和健康检查接口。
- 内部只读业务工具和预约草稿创建工具。

## 评测设计

Python/LangGraph 和 Dify 使用与 Spring AI 基线相同的人工整理用例进行评测。必须统计意图准确率、参数提取准确率、工具选择准确率、任务完成率、越权访问拦截、响应延迟和模型成本。

## 风险与权衡

- 额外网络跳转会提高延迟和失败概率。
  - 处理方式：显式超时、健康检查、有限重试和流式进度事件。
- Dify 和 Python 可能逐渐重复承担编排职责。
  - 处理方式：Dify 只负责外层工作流，LangGraph 负责有状态子 Agent。
- 知识更新可能分散到多个系统。
  - 处理方式：首版只保留一个权威知识源。
- 服务上下文令牌可能通过日志泄露。
  - 处理方式：短有效期、请求头白名单、日志脱敏，并且不向浏览器暴露。
- 旧 `dev` 基线缺少后续业务增强。
  - 处理方式：只迁移明确需要的非 AI 业务变更，不完整合并 `springai` 分支。

## 迁移与回滚计划

1. 在不改变现有预约行为的前提下增加 AI Gateway 和持久化结构。
2. 增加 Python 服务和只读工具。
3. 为首批只读场景启用 LangGraph 路由。
4. 增加 Dify 工作流和引擎切换。
5. 增加知识检索和预约草稿。
6. 完成评测后，再向选定角色开放 Agent 菜单。

回滚时关闭 Agent 菜单和引擎路由即可。原有实验室页面继续使用未修改的业务接口。
