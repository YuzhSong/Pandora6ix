# android-recipient-hierarchy Specification

## Purpose
Define organization-ID-based department, team and person navigation in the Android delegated-task recipient picker, including selection, search and legacy-response behavior.

## Requirements
### Requirement: Organizational recipient hierarchy
The Android delegated-task picker SHALL group authorized candidates by stable organization IDs into departments, their teams and individual people. Members attached directly to a department SHALL appear directly under that department. Different organization IDs SHALL remain distinct even if names match. A person SHALL appear at most once.

#### Scenario: Team and department members
- **WHEN** candidates include two teams under one department and a direct department member
- **THEN** the picker shows one department with two distinct child teams and its direct member, with each person under their actual organization.

#### Scenario: Same labels with different identities
- **WHEN** two departments or teams have identical display names but different IDs
- **THEN** their nodes and expansion state remain independent and their member lists are not merged.

#### Scenario: Missing department metadata
- **WHEN** a legacy response omits departmentId
- **THEN** the picker groups by the known orgUnitId without inventing parent relationships; candidates with no organization metadata remain visible in an unassigned group.

### Requirement: Expandable browsing and search
The picker SHALL initially collapse every organization level during unfiltered browsing and SHALL expand the ancestor path of all returned search matches. Group headers SHALL expose expand/collapse state and descendant counts. Selection SHALL remain attached to person IDs across query changes; choosing a person SHALL retain the picker and clear the search query.

#### Scenario: Browse a team
- **WHEN** the user opens the picker without a query and expands a department then a team
- **THEN** the team members become visible, while other groups retain their own collapsed state.

#### Scenario: Search and continue selection
- **WHEN** a search matches a team member and the user selects that person
- **THEN** the matching department and team are expanded for the result, the selected ID is retained after clearing the query, and the picker remains available for selecting another person.

### Requirement: Preserve candidate authorization and loading correctness
The picker SHALL use only the existing backend candidate response and its complete pagination. A failed load SHALL clear stale candidate results and show a retryable error. Journal behavior, task submission permissions and unrelated business APIs SHALL remain unchanged.

#### Scenario: Candidate request fails
- **WHEN** loading a changed query fails
- **THEN** old candidates are not presented as matching the new query and the user can retry without losing selected person IDs.
