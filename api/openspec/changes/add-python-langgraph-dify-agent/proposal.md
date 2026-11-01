# Change: 增加基于 FastAPI、LangChain、LangGraph 与 Dify 的实验室 Agent

## Why

`springai` 分支已经在 Java 单体中验证了角色隔离、工具调用、RAG、预约草稿和审计方案。`py-agent` 分支需要在不复制 Spring AI 实现的前提下，验证 Python AI 服务与低代码工作流平台接入同一套实验室业务系统的可行性，并为不同 Agent 实现提供可比较的工程基线。

## What Changes

- 在仓库根目录增加独立 Python AI 服务 `ai/`，使用 FastAPI、Pydantic、LangChain 和 LangGraph。
- 在 `ruoyi-biz` 增加不依赖 Spring AI 的 AI Gateway，负责会话、消息、引擎路由、工具调用审计和预约草稿。
- 增加面向 Python/Dify 的短期服务上下文令牌，用户身份和角色只能由 Spring Boot 生成。
- 增加只读实验室工具和唯一的临时写工具 `createReservationDraft`。
- 增加 Dify 安全问答与预约分析工作流，并通过服务端 HTTP 调用 LangGraph 子 Agent。
- 增加 Python 服务健康检查、超时、降级、幂等和统一错误响应。
- 建立与 `springai` 分支可比较的评测用例和指标口径。

## Impact

- Affected specs: `ai-gateway`, `python-agent`, `dify-workflow`, `tool-security`
- Affected code:
  - `api/ruoyi-biz/src/main/java/com/ruoyi/module/agent`
  - `api/ruoyi-biz/src/main/resources/mapper/module`
  - `ai/`
  - `ui/src/api/module`
  - `ui/src/views/module/agent`
  - `tools/sql-source/module`
- Existing laboratory business remains available when the AI service is stopped.
- This change does not merge `springai` agent code into `py-agent`.
