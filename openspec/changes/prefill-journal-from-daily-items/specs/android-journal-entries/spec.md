## ADDED Requirements

### Requirement: Editable daily-item Markdown defaults

When creating a journal without an applicable draft or existing journal, Android SHALL prefill one editable body with all daily items belonging to the current account and selected journal work date, in their stored order, regardless of completion status. Each item SHALL use a Markdown unordered-list marker, with multiline continuations indented and an empty paragraph after the list for additional free-form journal content. When no daily items match, the body SHALL remain empty. The template SHALL be ordinary editable text, not a placeholder or mandatory submission section, and SHALL NOT modify daily-item completion state.

#### Scenario: Include completed and incomplete items for the selected date
- **WHEN** the selected date has one completed and one incomplete todo and another date also has a todo
- **THEN** the new journal contains both selected-date todos as Markdown bullets and excludes the other date's todo

#### Scenario: Delete defaults and append independent content
- **WHEN** the user deletes any or all default bullets and writes additional work notes
- **THEN** the submitted body uses exactly the user's edited content without restoring bullets or restricting additional text

#### Scenario: Date without todos
- **WHEN** the selected journal date has no daily items
- **THEN** Android opens the usual empty journal body for free-form writing

### Requirement: Preserve journal edits over generated defaults

Android SHALL prioritize applicable date-specific drafts and existing journal entries over generated defaults, including a draft whose text the user has emptied. Editing or supplementing an existing journal SHALL NOT inject the template again. During one editing session, switching dates and returning SHALL preserve each date's user-edited content and tags without duplication or replacement. Successful publication and explicit discard SHALL clear the corresponding editor session and stale in-memory draft reference. Existing date eligibility, tags, entry identifiers, server conflict handling and publishing rules SHALL remain in effect.

#### Scenario: Restore edited draft
- **WHEN** an applicable draft has deleted todo text and added personal notes
- **THEN** opening the editor restores that draft as written without adding current todos

#### Scenario: Supplement a published journal
- **WHEN** the user edits or supplements an existing journal whose date still has todos
- **THEN** the existing ordered entries and identifiers are preserved without automatically adding a todo list

#### Scenario: Return to a date after clearing its template
- **WHEN** the user clears all default text, switches to another eligible date, and switches back
- **THEN** the first date's editor remains empty and retains its selected tags

#### Scenario: Publish and reopen
- **WHEN** the user publishes an edited template and reopens the journal
- **THEN** the published entries are loaded and a stale pre-publication draft is not restored
