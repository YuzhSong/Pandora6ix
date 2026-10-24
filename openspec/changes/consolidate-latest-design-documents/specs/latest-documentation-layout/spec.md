## ADDED Requirements

### Requirement: One current main document per subject
The documentation SHALL retain exactly the five user-selected main Markdown files and docs/README.md, with their filenames unchanged; meetings SHALL remain without meeting documents.

#### Scenario: Readers locate the latest documents
- **WHEN** a reader opens the docs index after consolidation
- **THEN** it links to API0.1, architecture A0.2, database DB0.2, complete requirements V1.3 and the V1.3 validation record without offering obsolete competing versions

### Requirement: Preserve current evidence before removing duplicates
The consolidation MUST preserve the current confirmation notes and planned interaction/acceptance design within the complete V1.3 document, without changing chapter 2 business rules or identifiers.

#### Scenario: Duplicate companion files are removed
- **WHEN** the standalone chapter, confirmation notes and interaction file are deleted
- **THEN** chapter 2 remains unchanged and the migrated notes, open questions and planned acceptance scenarios remain accessible as appendices

### Requirement: Keep references and validation truthful
Current Markdown file links MUST resolve to retained files, internal references MUST resolve to their anchors, and historical validation results SHALL be distinguished from the new consolidation checks.

#### Scenario: Earlier verification checked nine documents
- **WHEN** the current set contains six Markdown documents
- **THEN** the nine-document result is explicitly historical and new validation records the current six-document set without claiming runtime tests

### Requirement: Recoverable document-only cleanup
The change MUST delete only the ten inventoried obsolete or duplicate Markdown files, retain Git recovery history and placeholders, and leave Android, Web, Backend and prior OpenSpec artifacts unchanged relative to the synchronized dev baseline.

#### Scenario: Review the pull request
- **WHEN** backend-syz is compared with the synchronized dev baseline
- **THEN** the diff contains only docs and this OpenSpec change, and older documents remain recoverable from Git
