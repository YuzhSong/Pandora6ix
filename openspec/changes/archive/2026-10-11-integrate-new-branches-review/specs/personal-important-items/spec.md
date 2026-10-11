## ADDED Requirements

### Requirement: Stable candidate identity

The prototype SHALL identify each formal personal item by its source log id independently of editable display text. Candidate exclusion SHALL use the source id. Removing and reselecting an item within its eligible date window SHALL restore its edited text during the same application session. Separate source logs with equal content SHALL retain separate identities. The existing ten-item cap and inclusive ten-day source window SHALL remain enforced.

#### Scenario: Edit a selected item
- **WHEN** a user edits the text of a formal personal item
- **THEN** the original source log remains excluded from candidates and cannot be selected twice

#### Scenario: Remove and restore an edited item
- **WHEN** a user moves an edited item back to candidates and selects it again during the same session
- **THEN** its source identity and edited text are restored, including after leaving and revisiting the editor

#### Scenario: Equal text from distinct sources
- **WHEN** two eligible source logs contain identical text and one is selected
- **THEN** only that source is excluded and the other remains selectable

#### Scenario: Date boundary and maximum size
- **WHEN** a source log lies outside today and the preceding nine days, or ten formal items are already selected
- **THEN** the out-of-window log is absent from candidates and an eleventh formal item cannot be added
