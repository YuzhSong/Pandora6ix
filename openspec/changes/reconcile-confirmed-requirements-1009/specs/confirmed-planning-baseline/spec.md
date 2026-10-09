## ADDED Requirements

### Requirement: Deliver a consistent versioned planning baseline
The documentation SHALL preserve previous inputs and deliver synchronized requirement, architecture, database, API and interaction documents without changing runtime code or Git history.

#### Scenario: Revise after stakeholder confirmation
- **WHEN** the confirmed 2026-10-09 changes are documented
- **THEN** old sources remain unchanged, the full document contains the exact new second chapter and unresolved policies are explicitly identified

### Requirement: Plan daily journals with selectable work entries
The baseline SHALL specify at most one published journal per person per business date, multiple full work entries within it, retained draft/confirmation protection and personal important-item selection referencing owned recent-ten-day entries with at most ten choices.

#### Scenario: Select one work entry
- **WHEN** a user edits personal important items
- **THEN** the candidate is a full entry rather than a whole journal, selection is saved and reflected in the dashboard without changing the journal content

### Requirement: Separate personal completion from delegated acceptance
The baseline SHALL permit ordinary employees to create self-only personal tasks requiring direct-supervisor approval, with self-completion after approval, while preserving delegated-task recipient progress and creator acceptance.

#### Scenario: Ordinary employee creates a personal task
- **WHEN** the employee submits a self-only task and it is approved
- **THEN** the employee can mark it complete without a creator-acceptance request and cannot use that permission to assign tasks to other people

### Requirement: Define shared labels and direct task navigation
The baseline SHALL define enterprise-shared labels, department-manager/founder creation, Web administrator maintenance, one primary label with secondary labels, optional task/journal labeling, primary task color and all-selected-label matching. All calendar modes SHALL show tasks only and task clicks SHALL open details directly without automatic mode drilldown.

#### Scenario: Filter with two labels
- **WHEN** two distinct labels are selected
- **THEN** only authorized tasks containing both labels match, regardless of primary/secondary position

### Requirement: Reserve audit and setting contracts without implementing them
The documentation SHALL reserve dashboard-message approval contracts and related data, retain backend workflow states despite hiding the current-state block, remove only the journal mail entry, and define password/logout/font/theme settings.

#### Scenario: Read an approval message
- **WHEN** the future message entry references a task review
- **THEN** the contract requires reviewer authorization independently of message visibility and marks the entry as reserved rather than implemented
