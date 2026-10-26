## ADDED Requirements
### Requirement: Current organization membership
Organization member lists and direct-member counts SHALL exclude departed personnel while retaining disabled accounts that remain in service. Departure history SHALL remain accessible in personnel management. Display filtering SHALL NOT weaken server deletion or manager responsibility constraints.
#### Scenario: Departed department manager
- **WHEN** a department member is marked DEPARTED
- **THEN** organization management excludes that person from current members and counts while retaining other in-service members

### Requirement: Readable personnel history
Personnel history SHALL show numbered events from earliest to latest recording time, number job adjustments separately, and present changed values under explicit before and after headings.
#### Scenario: Two transfers followed by departure
- **WHEN** personnel history contains two job adjustments followed by departure
- **THEN** the first adjustment appears before the second adjustment and departure, each event has a visible sequence number, and before/after organization and supervisor values are separately labeled
