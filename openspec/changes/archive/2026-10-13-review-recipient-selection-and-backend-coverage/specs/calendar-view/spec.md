## ADDED Requirements

### Requirement: Recipient candidate query interaction
The existing delegated-task entry prototype SHALL support @-triggered recipient queries in a dedicated candidate layer, selection of one or more managed mock members, removal through selected-member chips, and explicit completion of candidate selection. The candidate layer SHALL scroll independently of the task form and remain reachable with the soft keyboard open. Each row tap SHALL toggle only that member. Existing recipient validation, ordered task tags and dirty-exit protection SHALL remain intact. The prototype SHALL NOT claim that mock candidates are server-authorized recipients.

#### Scenario: Query and select multiple recipients
- **WHEN** the user enters @ with an optional member keyword
- **THEN** matching mock members appear in the candidate layer, multiple selected members are retained as chips, and completing the candidate selection closes the layer

#### Scenario: Keyboard and row hit targets
- **WHEN** the keyboard is visible and the user scrolls or taps a candidate row
- **THEN** candidates remain reachable, the task form does not shift because candidate results change, and only the tapped member toggles

#### Scenario: Remove and validate
- **WHEN** the user removes selected-member chips and attempts to save with no recipient
- **THEN** the existing recipient validation prevents saving, and selected task tags still participate in dirty-exit protection

#### Scenario: Continue a dirty task after Android back
- **WHEN** Android back dismisses a dirty task sheet and the user chooses to continue filling it
- **THEN** the sheet becomes visible again with its title, recipients and tags retained
