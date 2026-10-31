## ADDED Requirements

### Requirement: Transient expired journal notice
Android SHALL show an expired journal edit notice for 2 seconds following each blocked click, preserving the selected date and draft.

#### Scenario: Notice expires
- **WHEN** a user selects an existing journal whose edit window has ended
- **THEN** the notice appears and automatically disappears after 2 seconds

#### Scenario: Repeated click
- **WHEN** the same blocked date is clicked again before or after dismissal
- **THEN** the notice appears again and its 2 second timeout restarts

#### Scenario: Valid date selected
- **WHEN** a user selects an editable date or closes the editor
- **THEN** the expired journal notice is cleared
