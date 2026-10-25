## ADDED Requirements

### Requirement: Role-aware assistant communication
The assistant MUST use role-appropriate language and MUST hide model maintenance details from students and teachers.

#### Scenario: Student submits a reservation draft
- **WHEN** a student creates a reservation draft
- **THEN** the assistant asks the student to verify the business fields and confirm on the reservation page without exposing frontend or backend terminology

#### Scenario: Non-admin views the assistant
- **WHEN** a student or teacher opens the assistant
- **THEN** model configuration, connection testing and backend restart instructions are not available

### Requirement: Role-scoped platform help
The assistant MUST answer platform usage questions from enabled knowledge visible to the current role.

#### Scenario: Student asks about device records
- **WHEN** a student asks what the device records page contains
- **THEN** the assistant explains the user-facing fields without exposing internal table or infrastructure details

#### Scenario: Admin asks about the assistant knowledge base
- **WHEN** an administrator asks what the assistant knowledge base is
- **THEN** the assistant explains its purpose and data boundaries instead of rejecting the question as out of scope

### Requirement: Progressive reservation guidance
The assistant MUST preserve and collect reservation conditions needed to produce a reservation draft.

#### Scenario: Student omits reservation time
- **WHEN** a student says they want to reserve a computer laboratory without a date or time
- **THEN** the assistant provides relevant current availability and asks for the missing date, time and participant count

#### Scenario: Student asks for rooms and devices together
- **WHEN** a student asks which laboratories and devices can be reserved
- **THEN** the assistant queries both categories and presents them as separate results

### Requirement: Page-only reservation confirmation
The assistant MUST NOT expose a model tool that confirms a reservation draft.

#### Scenario: Model attempts to submit a draft
- **WHEN** the model has created a reservation draft
- **THEN** it can only direct the user to the confirmation button and cannot confirm the draft itself

### Requirement: Assistant quality regression coverage
The evaluation baseline MUST cover role tone, platform help, compound intents and write-operation boundaries.

#### Scenario: Evaluation runs
- **WHEN** the offline or model-backed evaluation runs
- **THEN** it reports failures for technical-term leakage, missing platform-help routing or model-side reservation confirmation
