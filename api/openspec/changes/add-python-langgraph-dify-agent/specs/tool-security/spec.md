## ADDED Requirements

### Requirement: Trusted identity context
Internal Agent tools SHALL derive identity and role from a short-lived Spring Boot signed context.

#### Scenario: Model supplies another user ID
- **WHEN** model output or a Python request contains a different user ID
- **THEN** the tool SHALL ignore that value
- **AND** it SHALL use the identity in the signed context

### Requirement: Role-scoped tool allowlist
The system SHALL expose only tools allowed for the authenticated role.

#### Scenario: Student calls an administrator tool
- **WHEN** a student attempts to invoke a teacher or administrator query tool
- **THEN** the tool endpoint SHALL deny the call
- **AND** it SHALL record a denied audit event

### Requirement: Draft-only Agent write
The only Agent-accessible write operation SHALL create a temporary reservation draft.

#### Scenario: Model attempts direct confirmation
- **WHEN** model output requests confirmation of a reservation
- **THEN** no reservation SHALL be created
- **AND** confirmation SHALL remain an explicit browser action followed by Java business validation

### Requirement: Auditable tool execution
The system SHALL record tool name, request ID, session ID, user ID, result status, elapsed time, and a redacted argument summary.

#### Scenario: Tool execution fails
- **WHEN** an internal tool returns an error or times out
- **THEN** the failure SHALL be persisted with its trace ID
- **AND** secrets and full authentication headers SHALL NOT be stored

### Requirement: Idempotent retries
Agent-originated tool requests SHALL support an idempotency key.

#### Scenario: Dify retries draft creation
- **WHEN** the same request ID is delivered more than once
- **THEN** the system SHALL return the existing draft instead of creating a duplicate
