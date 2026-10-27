## ADDED Requirements

### Requirement: Confirm department and team deletion
Web SHALL offer deletion for non-root organization units, explain dependency blockers, and require confirmation naming the selected unit. A successful server response SHALL remove the unit from the tree and personnel filters and select its parent. Failed requests SHALL preserve directory data and display an actionable error. Confirmation SHALL prevent duplicate submission and accidental navigation while pending.

#### Scenario: Confirm empty unit deletion
- **WHEN** the administrator confirms deletion of an empty team
- **THEN** Web sends the unit identifier and displayed version, removes it only after success, and selects the parent department

#### Scenario: Cancel or failed deletion
- **WHEN** confirmation is cancelled or the server rejects deletion
- **THEN** the unit remains visible and a rejected request displays its reason
