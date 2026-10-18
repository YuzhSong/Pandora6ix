## ADDED Requirements

### Requirement: Concise personnel editing and actionable credential handoff
The personnel editor SHALL retain validation and actionable failures without persistent login-change, legacy-profile or generic supervisor explanation blocks. Bulk handoff SHALL be offered only when the current session holds deliverable credentials for existing non-admin personnel. It SHALL NOT list all historical accounts as missing passwords or enable downloading an empty list.

#### Scenario: Edit existing personnel
- **WHEN** an administrator opens a personnel editor
- **THEN** editable values and required indicators remain available without the redundant explanation blocks, and invalid saves still receive a concrete error

#### Scenario: No captured credentials
- **WHEN** the session has no deliverable credentials
- **THEN** bulk handoff is unavailable and no empty export dialog with a missing-password name wall is opened

#### Scenario: New or reset account
- **WHEN** the administrator has successfully created or reset an employee account
- **THEN** bulk handoff becomes available for the captured account and copy/download continue working without persisting plaintext passwords
