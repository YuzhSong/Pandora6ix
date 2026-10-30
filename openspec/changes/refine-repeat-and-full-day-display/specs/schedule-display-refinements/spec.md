## ADDED Requirements

### Requirement: Explicit custom repeat editor
Preset daily/weekly/monthly choices SHALL use interval 1 and hide the custom editor. Only selecting Custom SHALL reveal interval and unit inputs. Weekly weekday selection SHALL remain available for weekly rules. Draft restoration SHALL preserve the custom choice.

#### Scenario: Switch away from custom
- **WHEN** a custom interval of 3 is followed by selecting daily
- **THEN** the input is hidden and the submitted rule repeats every day with interval 1

### Requirement: Full-day portions of continuous tasks
Week view SHALL render continuous task portions covering 00:00 through at least 23:59 as top bars across those date columns. Partial first/last dates SHALL remain daily cards. Each date portion SHALL appear only once and original times SHALL remain unchanged.

#### Scenario: Long continuous interval
- **WHEN** a task runs from Jan 1 09:00 to Sep 10 10:00
- **THEN** Jan 2 through Sep 9 use top bars clipped to the visible week, while Jan 1 and Sep 10 use daily cards

#### Scenario: Midnight boundaries
- **WHEN** a task starts at midnight or ends at midnight
- **THEN** a fully covered start date uses a bar, and a zero-length final date has no card or bar
