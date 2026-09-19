## ADDED Requirements

### Requirement: Week task bars match Month readability
Week task bars SHALL use the same readable height and label size as Month task bars and SHALL not add ellipsis to labels.

#### Scenario: Inspect Week view
- **WHEN** a multi-day task is shown in Week view
- **THEN** its bar is tall enough for the shared label scale and visible text is clipped only at the actual bar boundary, without an ellipsis marker
