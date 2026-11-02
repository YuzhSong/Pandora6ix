## ADDED Requirements

### Requirement: Journal primary tag appears first
Android SHALL display the journal primary tag first in selected tags, the tag picker, journal cards and journal detail. Secondary tags SHALL retain their relative order. Editing SHALL preserve the primary tag using primaryTagId regardless of the returned tag array order.

#### Scenario: Switch primary during creation
- **WHEN** a user selects several tags and makes a later tag primary
- **THEN** that tag appears first in the picker and selected tags and is submitted as primary.

#### Scenario: Edit and display unordered tags
- **WHEN** the server returns secondary tags before the primary tag
- **THEN** cards, detail and editor display the primary tag first and saving retains its identity.

#### Scenario: No matching primary
- **WHEN** primaryTagId is absent or does not match an available tag
- **THEN** existing relative tag order is retained without adding a tag.
