## ADDED Requirements

### Requirement: Safe cursor tracking during journal input
Android SHALL only compute a journal cursor rectangle using a layout matching the current text after any frame suspension, with an offset inside that layout's text range.

#### Scenario: Input advances while waiting for a frame
- **WHEN** text changes from 9 to 10 characters before a cursor scroll resumes
- **THEN** the old 9-character layout is skipped rather than queried at offset 10

#### Scenario: Deletion or replacement during input
- **WHEN** current text differs from measured text, including same-length replacement
- **THEN** cursor tracking waits for matching layout without changing the input

#### Scenario: Matching layout
- **WHEN** current text and measured text match
- **THEN** cursor tracking uses a valid bounded offset, including empty text and end of text
