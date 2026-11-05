## ADDED Requirements

### Requirement: Journal list author profiles

Every journal list item SHALL include the author profile — `id`, `displayName`, `jobTitle`, `organizationName`, `departmentName` — identical to the detail response, so clients do not depend on locally cached member directories that omit disabled or cross-department authors. The list SHALL resolve author profiles in a bounded number of queries rather than one per item.

#### Scenario: List shows author names without local directory

- **WHEN** an authorized manager lists subordinate journals that include an author absent from the client's cached member directory
- **THEN** each item carries the author display name and the client shows the real name instead of a placeholder
