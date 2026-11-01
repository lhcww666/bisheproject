## ADDED Requirements

### Requirement: Server-side Dify invocation
The system SHALL invoke Dify only from a trusted server component.

#### Scenario: Browser inspects Agent traffic
- **WHEN** the Vue client communicates with the Agent
- **THEN** it SHALL NOT receive the Dify API key or unrestricted Dify workflow credentials

### Requirement: Dify outer workflow
Dify SHALL perform outer intent routing and may invoke a LangGraph sub-Agent for stateful reservation work.

#### Scenario: Reservation requires complex collection
- **WHEN** Dify determines that a reservation request is incomplete or requires multiple tool calls
- **THEN** Dify SHALL call the LangGraph reservation endpoint with the signed Agent context
- **AND** Dify SHALL return a structured draft or follow-up question

### Requirement: Versioned workflow exports
The repository SHALL contain exportable Dify workflow definitions for every required workflow.

#### Scenario: Recreate workflows in a clean environment
- **WHEN** a developer imports the repository workflow DSL into a compatible Dify instance
- **THEN** the safety-question and reservation-analysis workflows SHALL be recreated without repository secrets

### Requirement: Low-confidence knowledge response
The safety knowledge workflow SHALL avoid unsupported answers when retrieval confidence is insufficient.

#### Scenario: No relevant safety document
- **WHEN** retrieval returns no result above the configured threshold
- **THEN** the workflow SHALL state that no reliable source was found
- **AND** it SHALL direct the user to a laboratory administrator
