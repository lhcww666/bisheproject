## Context

`py-agent` is based on the core laboratory business in `dev`. Spring Boot owns JWT authentication, role and data-scope checks, reservation rules, review status transitions, safety-exam validity, credit thresholds, and transactional writes. The new AI components must not duplicate or bypass those rules.

The selected stack has separate responsibilities:

- FastAPI provides the Python service boundary, validation, streaming, and lifecycle management.
- LangChain provides model adapters, tool definitions, prompt components, output parsers, and optional knowledge-processing components.
- LangGraph provides stateful Agent orchestration, conditional routing, tool loops, and resumable multi-turn collection.
- Dify provides a server-side visual workflow layer and a knowledge/configuration surface.

## Goals / Non-Goals

- Goals:
  - Provide one Spring Boot entry point for browser clients regardless of the selected AI engine.
  - Support both a direct Python/LangGraph path and a Dify-outer/LangGraph-inner path.
  - Keep all business writes in Java services with the original transaction checks.
  - Make every Agent run and tool call traceable.
  - Keep the original business system usable when AI dependencies fail.
- Non-Goals:
  - Allow the model or Dify to access MySQL business tables directly.
  - Allow the model to approve reviews, change credit, adjust inventory, or confirm reservations.
  - Support unrestricted multi-agent collaboration in the first release.
  - Maintain two independent production knowledge bases in the first release.
  - Replace the `springai` branch implementation.

## Decisions

### Decision: Spring Boot remains the control plane

The browser authenticates only with the existing JWT. Spring Boot resolves the current user and role, applies endpoint permissions, creates the Agent request context, and exposes narrowly scoped internal tools.

Alternative considered: let Python call public user APIs with the browser token. Rejected because it increases token exposure, couples Python to all browser API shapes, and makes authorization harder to audit.

### Decision: Use short-lived signed service context

Spring Boot creates a short-lived signed context for each Agent request. It includes `userId`, role, session ID, trace ID, expiration, and the allowed tool set. Python and Dify may only pass this context to internal tool endpoints; they cannot invent or modify the user identity.

The signing secret is supplied through an ignored local file or environment variable. Internal tool endpoints are not exposed by the public reverse proxy.

### Decision: Use the same public AI contract for both engines

The Spring Boot AI Gateway normalizes requests and responses for the `python` and `dify` engines. Both paths emit the same message, tool, citation, draft, completion, and error events.

Alternative considered: let the frontend call each engine directly. Rejected because sessions, audits, permissions, and business drafts would be fragmented across clients and platforms.

### Decision: LangChain is a component layer inside LangGraph

LangChain supplies the model adapter, tool schemas, prompt composition, parser, and later RAG components. LangGraph owns state transitions and multi-turn behavior. No separate LangChain-only agent implementation is required.

### Decision: Dify is the outer workflow layer

For the Dify path, Spring Boot calls a Dify Workflow API. Dify performs intent routing and simple validation, then calls the LangGraph reservation sub-Agent through an HTTP node. Dify does not receive database credentials and cannot execute business writes.

### Decision: One canonical knowledge source

The first release uses Dify Knowledge as the canonical reviewed safety-knowledge source. Python retrieves knowledge through a server-side retrieval tool. A later change may add a BGE/Qdrant retriever for comparison, but Dify Knowledge and the Python vector store must not both be authoritative for the same documents.

## Architecture

```text
Vue Agent Page
    |
    | JWT / SSE
    v
Spring Boot AI Gateway
    |-- session and message persistence
    |-- engine routing
    |-- signed Agent context
    |-- audit and draft endpoints
    |
    +--> FastAPI + LangGraph + LangChain
    |
    +--> Dify Workflow --> LangGraph sub-Agent
    |
    +--> internal controlled tools --> business services --> MySQL
```

## Python Service Structure

The Python project follows the flat layout used by the existing FastAPI reference project:

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

`routers` contains HTTP boundaries. `services` contains use cases. `agent` contains LangGraph state and graph construction. `tools` contains model-callable tools. `integrations` contains Java, LLM, and Dify clients. The Python service does not contain business database models or SQL.

## Agent State

The first LangGraph state contains:

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

The first graph supports knowledge questions, read-only business queries, and reservation parameter collection. Redis may store short-lived checkpoints keyed by session ID; MySQL remains the durable source for user-visible messages.

## Data Ownership

- Spring Boot and MySQL own sessions, messages, Agent runs, tool-call audit records, reservation drafts, and all laboratory business data.
- Python owns only transient execution state and optional Redis checkpoints.
- Dify owns workflow definitions and the canonical knowledge documents for the first release.
- Dify workflow exports are versioned under `ai/dify/workflows/`; runtime database volumes are not committed.

## Security and Failure Handling

- Tool calls require a valid signed context and role allowlist.
- Only `createReservationDraft` may create a temporary record. Reservation confirmation remains a user-facing Spring Boot operation.
- Tool retries use an idempotency key based on request ID and session ID.
- Model prompts and user text never grant authorization.
- Timeouts are bounded for Java-to-Python, Java-to-Dify, and Python-to-tool calls.
- If Python or Dify is unavailable, the Gateway returns a recoverable error and keeps the original business pages usable.
- Secrets, tokens, and full internal tool payloads are excluded from normal logs.

## API Surface

The detailed contract is documented in `docs/13-AI-Agent接口设计说明书.md`. The first implementation includes:

- Browser-facing session, history, streaming chat, status, and draft confirmation endpoints.
- FastAPI chat, reservation analysis, and health endpoints.
- Internal read-only business tools and the draft-creation tool.

## Evaluation

Python/LangGraph and Dify are evaluated against the same curated cases used for the Spring AI baseline. Required measurements are intent accuracy, parameter extraction, tool-selection accuracy, task completion, unauthorized-access blocking, response latency, and model cost.

## Risks / Trade-offs

- Extra network hops increase latency and failure modes.
  - Mitigation: explicit timeouts, health checks, bounded retries, and streamed progress events.
- Dify and Python can both evolve into duplicate orchestration layers.
  - Mitigation: Dify owns only the outer workflow; LangGraph owns stateful sub-Agent behavior.
- Knowledge updates can split across systems.
  - Mitigation: one canonical knowledge source in the first release.
- Service context tokens may leak through logs.
  - Mitigation: short expiration, header allowlists, log redaction, and no browser exposure.
- The old `dev` baseline lacks later business enhancements.
  - Mitigation: port only explicitly selected non-AI business changes instead of merging the complete `springai` branch.

## Migration Plan

1. Add the AI Gateway and persistence schema without changing existing booking behavior.
2. Add the Python service and read-only tools.
3. Enable LangGraph routing for read-only scenarios.
4. Add Dify workflows and engine switching.
5. Add knowledge retrieval and reservation drafts.
6. Run evaluation and only then expose the Agent menu to selected roles.

Rollback disables the Agent menu and engine routing. Existing laboratory pages continue to use the unchanged business APIs.
