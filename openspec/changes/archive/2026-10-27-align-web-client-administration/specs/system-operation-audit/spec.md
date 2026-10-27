## Purpose

Define system-operation traceability separately from employee work journals, including task and journal mutations and an honest pending state until the client chooses storage and the backend supplies trustworthy events.

## ADDED Requirements

### Requirement: Pending audit integration
The Web SHALL provide a system-log destination identifying collection as pending confirmation. It SHALL identify task and journal create/update/delete operations as required coverage. It SHALL NOT fabricate events, claim recording has started, request employee journal content, or choose file-log versus database storage before client agreement.

#### Scenario: Open before backend integration
- **WHEN** an administrator opens system logs before the client confirms storage
- **THEN** the page shows a pending status and intended operation coverage, without a misleading empty event table
