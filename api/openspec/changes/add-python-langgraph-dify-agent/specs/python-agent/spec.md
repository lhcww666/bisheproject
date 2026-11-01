## ADDED Requirements

### Requirement: Read-only business queries
The Python Agent SHALL answer authorized laboratory queries by invoking internal Spring Boot tools rather than accessing MySQL directly.

#### Scenario: Student checks personal reservations
- **WHEN** a student asks for their own reservation records
- **THEN** the Agent SHALL call the current-user reservation tool
- **AND** the answer SHALL use the returned business data

### Requirement: Stateful reservation collection
The LangGraph Agent SHALL retain enough state to collect missing reservation fields across turns.

#### Scenario: Reservation time is missing
- **WHEN** a student requests a reservation without specifying a time
- **THEN** the Agent SHALL ask for the missing time
- **AND** the next turn in the same session SHALL continue the reservation flow without restarting

### Requirement: LangChain component boundary
The Python service SHALL use LangChain for model invocation, tool schemas, prompt composition, and parsing while LangGraph owns workflow transitions.

#### Scenario: Model selects a business tool
- **WHEN** the Agent needs laboratory data
- **THEN** the model output SHALL be structured as a registered LangChain tool call
- **AND** the LangGraph tool node SHALL validate and execute it through the Java API client

### Requirement: Bounded Python service API
The Python service SHALL expose versioned health, chat, streaming, and reservation-analysis endpoints.

#### Scenario: Service health is queried
- **WHEN** Spring Boot calls the Python health endpoint
- **THEN** the service SHALL report readiness without exposing secrets
