# 变更：增加基于 FastAPI、LangChain、LangGraph 与 Dify 的实验室 Agent

## 背景与问题

`springai` 分支已经在 Java 单体中验证了角色隔离、工具调用、RAG、预约草稿和审计方案。`py-agent` 分支需要在不复制 Spring AI 实现的前提下，验证 Python AI 服务与低代码工作流平台接入同一套实验室业务系统的可行性，并为不同 Agent 实现提供可比较的工程基线。

## 变更内容

- 在仓库根目录增加独立 Python AI 服务 `ai/`，使用 FastAPI、Pydantic、LangChain 和 LangGraph。
- 在 `ruoyi-biz` 增加不依赖 Spring AI 的 AI Gateway，负责会话、消息、引擎路由、工具调用审计和预约草稿。
- 增加面向 Python 和 Dify 的短期服务上下文令牌，用户身份和角色只能由 Spring Boot 生成。
- 增加只读实验室工具和唯一的临时写工具 `createReservationDraft`。
- 增加 Dify 安全问答与预约分析工作流，并通过服务端 HTTP 调用 LangGraph 子 Agent。
- 增加 Python 服务健康检查、超时、降级、幂等和统一错误响应。
- 建立与 `springai` 分支可比较的评测用例和指标口径。

## 影响范围

- 受影响规格：`ai-gateway`、`python-agent`、`dify-workflow`、`tool-security`
- 受影响代码：
  - `api/ruoyi-biz/src/main/java/com/ruoyi/module/agent`
  - `api/ruoyi-biz/src/main/resources/mapper/module`
  - `ai/`
  - `ui/src/api/module`
  - `ui/src/views/module/agent`
  - `sql/module`
- AI 服务停止时，原有实验室业务仍须可用。
- 本次变更不会把 `springai` 分支的 Agent 代码合并到 `py-agent`。
