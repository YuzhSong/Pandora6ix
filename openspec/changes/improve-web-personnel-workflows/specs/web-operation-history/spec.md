## ADDED Requirements
### Requirement: Operation log header filtering
The log table SHALL show named time, operation record, module, source, result and detail columns. Date, action, module, source and result filters SHALL be available from their corresponding headers, remaining available during loading, empty and error states. Filters SHALL preserve server-side paging, module/action compatibility and complete filtered exports. Keyword search and reset SHALL remain above the table.
#### Scenario: Filter failures and recover an empty result
- **WHEN** an administrator selects failure from the result header and no records match
- **THEN** the result header remains available to clear or change the selection, and any export uses the same selected filters
