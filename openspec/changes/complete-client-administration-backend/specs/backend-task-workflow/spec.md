## ADDED Requirements

### Requirement: Administrative role does not grant tag business authority
Pure ADMIN SHALL NOT list, create, edit or delete shared enterprise tags or access private tasks, journals or statistics. FOUNDER and DEPARTMENT_MANAGER SHALL maintain tags within their enterprise; other business users SHALL retain tag query access and cannot elevate permissions through ADMIN.

#### Scenario: Pure administrator requests business routes
- **WHEN** an ADMIN-only account requests tags, tasks, journals or statistics
- **THEN** the backend returns 403 without exposing or modifying business data.

### Requirement: Stable department metadata for recipient grouping
Authorized recipient candidates SHALL include nullable departmentId and departmentName determined from the candidate's company-scoped organization ancestry, while retaining orgUnitId and organizationName. Same-named departments SHALL remain distinct by ID. Scope, search, stable pagination and absence of sensitive personnel fields SHALL remain enforced. Android SHALL group candidates by department ID and retrieve all candidate pages before showing a complete list.

#### Scenario: Team member candidate
- **WHEN** a business manager searches a candidate assigned to a team under a department
- **THEN** the result contains that team's existing metadata and its parent department ID/name, enabling department grouping.

#### Scenario: More than one page
- **WHEN** an authorized manager has more candidates than one response page
- **THEN** each eligible candidate remains reachable and the Android picker follows pagination without dropping later candidates.
