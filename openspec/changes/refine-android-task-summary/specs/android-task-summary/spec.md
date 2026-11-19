## ADDED Requirements

### Requirement: Flat and filterable task table

Android SHALL show one row per task with its tags in a table column, placing the primary tag first when present. Each tag SHALL use the shared rounded chip with its configured color and visible name. Chips SHALL wrap when the column is too narrow. The first column SHALL have a static task name header. The tags, deadline, priority, completion status and owner headers SHALL offer dropdown filters with a clear option. The table SHALL omit the progress note column and the scrolling hint.

#### Scenario: Filter across columns

- **WHEN** the user selects a tag and a completion state
- **THEN** only loaded rows matching both values are shown in the current scope, with additional pages still available when provided by the API

#### Scenario: Keep scope selections

- **WHEN** the user switches scopes or rotates and returns to a filtered table
- **THEN** the scope retains its filters and matching task rows open the existing details

### Requirement: Task summary orientation lifecycle

Android SHALL request landscape when the user chooses landscape view, use a compact layout for wide or short windows, and restore the previous orientation policy when the request ends. The table SHALL remain usable when the system keeps the current orientation.

#### Scenario: Rotate and return

- **WHEN** the user requests landscape, then turns it off or leaves task summary
- **THEN** the summary preserves its scope during rotation and releases its orientation request on return
