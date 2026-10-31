## ADDED Requirements

### Requirement: Three distinct overview panels
The task-summary overview SHALL display exactly three single white surface panels: tasks, direct reports and recent journal activity. Each SHALL have a visible rounded boundary and spacing using existing Pandora styling, with no statistics or charts.

#### Scenario: Overview appearance
- **WHEN** the summary opens
- **THEN** its task controls and preview belong to one panel, followed by separate people and journal panels

### Requirement: Task filtering stays inside the first panel
The first panel SHALL show mine, created and managed as equal-width internal filters, defaulting to mine. A selection SHALL change only its task preview and SHALL NOT navigate to another page. The preview SHALL contain at most three real authorized matching tasks.

#### Scenario: Change filter
- **WHEN** a user selects “我创建的”
- **THEN** the same overview remains visible with the task preview updated and the people and journal panels retained

### Requirement: Task-only full list
The task panel SHALL provide “查看全部任务”, opening one task-only paginated destination for the selected scope. It SHALL reuse the full loaded collection and existing detail navigation. Returning SHALL preserve the overview filter and scroll.

#### Scenario: More than three tasks
- **WHEN** at least four tasks exist in the selected authorized collection
- **THEN** the overview displays at most three and the full list can display the fourth and continue paging without showing people or journals

### Requirement: Bounded people and journal previews
The overview SHALL show at most four DIRECT members in one horizontal row and at most three recent journals with existing one-line summaries. Each panel SHALL link to its complete existing destination. Overview panels SHALL NOT expand through inline pagination; retry SHALL remain available for failed reads.

#### Scenario: Return after full-list paging
- **WHEN** additional journal or candidate pages have been loaded in a full destination and the user returns
- **THEN** the overview still displays at most four people and three journals

#### Scenario: Partial candidate result
- **WHEN** the current candidate page has no DIRECT members but has another cursor
- **THEN** the overview explains that more can be viewed in the full organization instead of claiming no reports or expanding inline

### Requirement: Consistent member and secondary-page boundaries
Member detail SHALL place personal information, responsible tasks and recent journals in three separate white panels. Task and journal entries SHALL have individual visible boundaries and spacing. Member task/journal panels SHALL show at most three previews, with full lists retaining that member's executor/author filter. Complete task, journal and organization pages SHALL also have a visible white content panel.

#### Scenario: Member filtered full list
- **WHEN** a user opens all responsible tasks or journals from a member panel and returns
- **THEN** the full list contains only that authorized member's matching records, back restores the member page, and the overview task filter remains unchanged

### Requirement: Compact task presentation

Task entries SHALL preserve their visible boundaries and real title, status, tags, priority, due date and executor avatars with compact line heights and spacing. Normal browsing SHALL NOT include explanatory scope or implementation captions below filters or organization headings.

#### Scenario: Read a compact task
- **WHEN** a task with tags and executors is displayed
- **THEN** its fields remain readable within a shorter row and tapping it opens the existing task detail

### Requirement: Authorized member telephone

Member detail SHALL read an existing employee telephone through `GET /users/{id}/contact`. The server SHALL allow only business managers reading active members in their existing management scope and enterprise. The response SHALL contain only id and nullable phoneNumber. Candidate lists SHALL continue excluding phoneNumber and other personnel secrets. The client SHALL distinguish an absent telephone from a failed/unsupported request and SHALL allow retry without blocking tasks or journals.

#### Scenario: Managed employee with telephone
- **WHEN** a manager opens an active authorized member whose database record has phone_number
- **THEN** the personal-information panel displays that actual telephone

#### Scenario: Access outside management scope
- **WHEN** a manager requests an unknown, inactive, cross-enterprise or out-of-scope member
- **THEN** the contact endpoint returns uniform 404 without personnel fields; employees and pure administrators receive 403

#### Scenario: Missing telephone or unavailable contact read
- **WHEN** the contact read returns null or fails
- **THEN** the client shows “未填写” for null and a distinct unavailable/error state for failure, with retry
