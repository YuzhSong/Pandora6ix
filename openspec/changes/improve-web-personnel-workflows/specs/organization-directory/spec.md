## ADDED Requirements
### Requirement: Compact personnel profile
Personnel details SHALL group identity, organization, basic profile and account information, keep identity/status/roles together, display read-only information as text and mask identity numbers by default with an explicit reveal control. Active direct reports SHALL be counted and expandable. Credential handoff details SHALL load only when explicitly expanded in the personnel profile. Account reset SHALL remain an explicit action in the account section.
#### Scenario: Review a personnel profile
- **WHEN** an administrator opens a person's details
- **THEN** organization information precedes personal and account information, the identity number is masked, no credential handoff request is made until expansion, and the footer contains close and applicable edit actions

### Requirement: Comparable personnel views
Web SHALL retain grouped cards and offer a compact paginated table over the same filtered personnel, displaying organization paths, applicable supervisors, roles and separate employment/account states. It SHALL show matching and total counts and reset every active filter including employment state.
#### Scenario: Reset departed search
- **WHEN** an administrator filters departed personnel then resets filters
- **THEN** all personnel are visible, search and organization/role filters are cleared and matching/total counts agree
### Requirement: Explicit organization count scope
Organization management SHALL label direct and inclusive descendant personnel counts separately, excluding departed personnel from both counts and counting each in-service account once.
#### Scenario: Department with team members
- **WHEN** a department contains one direct member and two team members
- **THEN** it shows one direct and three inclusive members
