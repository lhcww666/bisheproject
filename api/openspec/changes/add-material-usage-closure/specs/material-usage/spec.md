## ADDED Requirements

### Requirement: Material inventory maintenance
Administrators MUST maintain material profiles and MUST change stock only through auditable stock adjustments.

#### Scenario: Administrator adjusts stock
- **WHEN** an administrator records an inbound or outbound stock adjustment with a positive quantity and reason
- **THEN** the system updates the material stock atomically and writes a stock record containing the type, signed quantity, before stock, after stock and operator

#### Scenario: Outbound adjustment exceeds stock
- **WHEN** an administrator attempts to reduce stock below zero
- **THEN** the system rejects the adjustment and leaves both stock and stock records unchanged

### Requirement: Reservation-bound material application
Students MUST apply only for active materials belonging to the laboratory of their own pending or approved reservation.

#### Scenario: Student submits an eligible application
- **WHEN** a student selects one of their pending or approved reservations and an active material in the same laboratory
- **THEN** the system creates a pending material application with a unique application number

#### Scenario: Student submits a duplicate active application
- **WHEN** the same reservation and material already has a pending or approved application
- **THEN** the system rejects the new application

### Requirement: Auditable material approval
Administrators MUST review material applications once, and inventory deduction MUST never make stock negative.

#### Scenario: Approval has sufficient stock
- **WHEN** an administrator approves a pending application and stock is sufficient
- **THEN** the system marks the application approved, deducts the requested quantity and writes an `APPLY_OUT` stock record

#### Scenario: Approval has insufficient stock
- **WHEN** an administrator approves a pending application but stock is insufficient
- **THEN** the system rejects the application without deducting stock

#### Scenario: Duplicate approval
- **WHEN** an administrator reviews an application that is no longer pending
- **THEN** the system rejects the operation and does not change stock

### Requirement: Stock restoration on reservation cancellation
Canceling a reservation MUST close its pending material applications and return all issued materials in the same transaction.

#### Scenario: Cancellation returns issued material
- **WHEN** a student cancels a reservation that has an approved material application
- **THEN** the system adds the issued quantity back to stock, marks the application returned and writes a `CANCEL_RETURN` stock record

#### Scenario: Cancellation closes pending application
- **WHEN** a student cancels a reservation that has a pending material application
- **THEN** the system marks the application canceled without changing stock

### Requirement: Role-scoped material pages
The system MUST expose material maintenance and review pages to administrators and material application pages to students while preserving server-side data scope.

#### Scenario: Student views material applications
- **WHEN** a student opens the material application page
- **THEN** the page and API return only applications submitted by that student

#### Scenario: Administrator reviews applications
- **WHEN** an administrator opens the material application list
- **THEN** the page allows viewing pending applications and submitting an approval or rejection with a review comment
