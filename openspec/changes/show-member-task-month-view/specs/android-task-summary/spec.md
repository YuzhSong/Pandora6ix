## ADDED Requirements

### Requirement: Read-only member task month view

Android SHALL show approved tasks assigned to the selected member in a month view directly within member details, using only the existing authorized managed task scope. The calendar SHALL default to the current business month and use the company month layout rules for date placement, multi-day bars, tag colors, status labels, and daily overflow.

#### Scenario: Open and browse the member calendar

- **WHEN** an authorized manager opens a member or switches the displayed month
- **THEN** Android loads all pages for that date range and shows tasks with an authorized assignment to that member, including tasks spanning the month boundary

#### Scenario: Inspect a date or task

- **WHEN** the manager taps a date, an overflow count or a task
- **THEN** Android offers the corresponding tasks or task details for reading and does not offer creation, editing, execution updates or acceptance from the member flow

#### Scenario: Partial data or request failure

- **WHEN** the month is loading or a page fails
- **THEN** Android displays a loading or retry state before claiming the selected month has no tasks

## MODIFIED Requirements

### Requirement: Authorized people and organization
The direct-report panel SHALL be a single horizontal row of candidate records marked DIRECT, with more linking to an expandable organization page constructed only from authorized server department/team IDs. Members SHALL open a detail with available identity fields, responsible tasks in a read-only month view, and recent journals. Unsupported contact/rank/reporting data SHALL be disclosed without fabrication or admin API access.

#### Scenario: Partial candidate page
- **WHEN** more candidate pages exist but no DIRECT person has been loaded
- **THEN** the UI discloses the partial result and permits loading more rather than claiming the user has no direct reports

#### Scenario: Member responsibility
- **WHEN** a member detail loads the selected month
- **THEN** only approved tasks with that member in authorizedAssignments are shown after all pages for the month are loaded
