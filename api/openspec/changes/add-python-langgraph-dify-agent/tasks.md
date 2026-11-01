## 1. Design and Contract

- [x] 1.1 Write the change proposal.
- [x] 1.2 Write the technical design and security boundaries.
- [x] 1.3 Write the Python Agent architecture and interface documents.
- [ ] 1.4 Review and approve this OpenSpec change.

## 2. Spring Boot AI Gateway

- [ ] 2.1 Add Agent session, message, run, tool-call, and reservation-draft persistence.
- [ ] 2.2 Add session ownership and menu permissions.
- [ ] 2.3 Add signed short-lived Agent context generation and validation.
- [ ] 2.4 Add Python and Dify engine routing with timeout and fallback.
- [ ] 2.5 Add normalized SSE events for message, tool, citation, draft, done, and error.
- [ ] 2.6 Add rate limiting, request IDs, and log redaction.

## 3. Internal Business Tools

- [ ] 3.1 Expose current-user reservation, safety-exam, and credit queries.
- [ ] 3.2 Expose available-room and available-device queries.
- [ ] 3.3 Expose reservation-review explanation queries.
- [ ] 3.4 Expose the draft-creation tool only to the student role.
- [ ] 3.5 Add role allowlists, idempotency, and tool audit records.
- [ ] 3.6 Reject all model-originated reservation confirmation and approval attempts.

## 4. Python Service

- [ ] 4.1 Create the FastAPI service skeleton and unified response handling.
- [ ] 4.2 Configure the LangChain model adapter and environment-based secrets.
- [ ] 4.3 Add health, chat, streaming, and reservation-analysis endpoints.
- [ ] 4.4 Add LangGraph state, intent routing, tool nodes, and response nodes.
- [ ] 4.5 Add multi-turn missing-field collection keyed by session ID.
- [ ] 4.6 Add service-context validation and Java tool client.
- [ ] 4.7 Add structured logs and trace propagation.

## 5. Dify Workflow

- [ ] 5.1 Create the safety knowledge question workflow.
- [ ] 5.2 Create the reservation analysis workflow.
- [ ] 5.3 Add an HTTP node that calls the LangGraph reservation sub-Agent.
- [ ] 5.4 Pass signed per-request context without exposing it to the browser.
- [ ] 5.5 Export workflow DSL under `ai/dify/workflows/`.
- [ ] 5.6 Test Dify failure fallback to the Python path.

## 6. Knowledge and Draft Closure

- [ ] 6.1 Load reviewed safety documents into the canonical Dify knowledge source.
- [ ] 6.2 Add role-aware retrieval and source citations.
- [ ] 6.3 Add a low-confidence no-answer path.
- [ ] 6.4 Generate a structured reservation draft from LangGraph.
- [ ] 6.5 Add browser confirmation and Java revalidation before reservation creation.
- [ ] 6.6 Add draft expiration and cancellation.

## 7. Frontend

- [ ] 7.1 Add the Agent API client and route/menu integration.
- [ ] 7.2 Add session and message panels.
- [ ] 7.3 Add SSE status rendering for tool calls and citations.
- [ ] 7.4 Add reservation draft confirmation UI.
- [ ] 7.5 Add unavailable-service and retry states.

## 8. Verification

- [ ] 8.1 Test normal, invalid, and unauthorized internal tool calls.
- [ ] 8.2 Test cross-user session and draft isolation.
- [ ] 8.3 Test incomplete reservation requests and multi-turn collection.
- [ ] 8.4 Test that the model cannot create, approve, cancel, or modify business records directly.
- [ ] 8.5 Test Python and Dify outage fallback.
- [ ] 8.6 Run the shared Agent evaluation set and record metrics.
- [ ] 8.7 Verify no secret or runtime volume is tracked by Git.
- [ ] 8.8 Update as-built documentation after implementation.
