## ADDED Requirements

### Requirement: Unified Agent entry point
The system SHALL provide one Spring Boot entry point for browser Agent requests regardless of whether the selected engine is Python or Dify.

#### Scenario: Switch engine without frontend changes
- **WHEN** an administrator changes the configured Agent engine
- **THEN** the browser SHALL continue using the same session and chat endpoints
- **AND** the response SHALL use the same normalized event contract

### Requirement: User-owned sessions
The system SHALL bind every Agent session to the authenticated user.

#### Scenario: Other user opens a session
- **WHEN** a user requests a session owned by another user
- **THEN** the system SHALL deny access
- **AND** the session and message contents SHALL NOT be returned

### Requirement: Normalized streaming events
The system SHALL stream Agent progress as typed server-sent events.

#### Scenario: Tool call during a streamed answer
- **WHEN** the Agent invokes a business tool while generating an answer
- **THEN** the stream SHALL emit a tool event with status and trace ID
- **AND** the stream SHALL finish with exactly one terminal done or error event

### Requirement: Graceful AI dependency failure
The system SHALL keep ordinary laboratory business available when Python or Dify is unavailable.

#### Scenario: Python service timeout
- **WHEN** the Python Agent service exceeds its configured timeout
- **THEN** the Gateway SHALL return a recoverable Agent error
- **AND** ordinary room, reservation, review, safety-exam, and credit pages SHALL remain usable
