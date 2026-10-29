## ADDED Requirements
### Requirement: Focused account delivery
Web SHALL default delivery to AVAILABLE accounts with search, explicit selection, masked passwords and concise empty states. Unavailable accounts SHALL appear only in optional collapsed status details. Single/batch creation SHALL offer delivery of confirmed successful person IDs. Every copy/export SHALL revalidate selected IDs. Export SHALL NOT imply receipt, reset passwords or send messages.
#### Scenario: Deliver the current creation batch
- **WHEN** a batch partially succeeds and the administrator exports its creation results
- **THEN** only confirmed successful person IDs are revalidated and exported, never all available company accounts
#### Scenario: Selected account becomes unavailable
- **WHEN** a selected employee changes password or the credential expires before export
- **THEN** fresh validation prevents that credential from being copied or downloaded and prompts the administrator to refresh the selection

## MODIFIED Requirements
### Requirement: Excel handoff and password column
The handoff table SHALL mask temporary passwords by default and show plaintext only for AVAILABLE employees after explicit reveal. Download SHALL produce a genuine .xlsx workbook containing name, complete login account identifier and temporary password as text cells, without a separate company login identifier column. Employee-selected passwords SHALL never enter the workbook. Copy SHALL use the complete login identifier and plain-text initial-password format.
#### Scenario: Deliver readable credentials in Excel
- **WHEN** an administrator downloads selected current handoff accounts
- **THEN** Excel can open the workbook, account and password characters are preserved as text, and only revalidated selected accounts are included
