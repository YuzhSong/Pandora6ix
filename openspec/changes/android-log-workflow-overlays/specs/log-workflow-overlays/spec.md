## ADDED Requirements

### Requirement: Logs separates today and history
The Work Logs section SHALL show today's entries separately from history, with a date filter and five-item default history limit that can expand to ten newest entries.

#### Scenario: Browse work logs
- **WHEN** the user opens Work Logs without a filter
- **THEN** today's logs appear first, history shows the five newest entries, and 展开 reveals up to ten entries

### Requirement: Tasks use a five-item preview
The Task Management section SHALL show five tasks initially and provide 展开 to reveal the remaining mock tasks.

#### Scenario: Browse tasks
- **WHEN** the user opens Task Management
- **THEN** five tasks are visible until the user taps 展开

### Requirement: Employee permissions are explicit
Ordinary employees SHALL not be able to dispatch tasks or approve reports.

#### Scenario: Tap restricted entry
- **WHEN** an ordinary employee taps 任务派发 or 待我审核
- **THEN** a compact overlay explains that the current role has no permission

### Requirement: Create and notifications use overlays
New log entry and notifications SHALL appear in layered UI over the current page without replacing the entire page.

#### Scenario: Add a log or read mail
- **WHEN** the user taps 记录今天的工作 or the mail button
- **THEN** a centered compact form or a right-side half-width drawer appears above a scrim
