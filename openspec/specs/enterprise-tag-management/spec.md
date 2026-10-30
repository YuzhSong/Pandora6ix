# enterprise-tag-management Specification

## Purpose
Define the separation between the removed Web administrator tag catalog and authorized enterprise-shared tag operations in business clients.

## Requirements
### Requirement: No Web administrator tag destination
The Web administration workspace SHALL expose no tag maintenance page and SHALL NOT load shared tags while loading administrator directory data. An ADMIN role alone SHALL NOT grant business tag maintenance authority. The earlier local-demo administrator tag catalog SHALL NOT be restored by specification synchronization.

#### Scenario: Open an old tag-management URL
- **WHEN** an administrator opens an obsolete tag destination
- **THEN** the Web client returns to organization management without loading or changing shared tags

### Requirement: Enterprise-scoped business tag operations
Business clients SHALL use the authenticated server tag catalog with enterprise-scoped IDs, nonempty names and valid hexadecimal colors. Creation and maintenance SHALL follow backend role checks for founders and department managers. The existing Android tag deletion command SHALL use backend reference cleanup; task and journal live tag references SHALL be removed and matching primary tags cleared without modifying historical JSON snapshots. No hierarchical tag parent model or disabling command SHALL be inferred.

#### Scenario: Delete a tag from the business client
- **WHEN** an authorized founder or department manager confirms removal of a tag in their enterprise
- **THEN** the existing backend command removes that tag and its live task/journal references while preserving historical snapshots

### Requirement: Use actual mutation feedback
Clients SHALL report backend validation, permission and version-conflict failures without claiming a successful local-demo save. An unsuccessful command SHALL retain actionable feedback rather than overwrite server-owned records.

#### Scenario: Permission rejected
- **WHEN** a user without tag-maintenance authority attempts a tag mutation
- **THEN** the command is rejected and the client does not claim success
