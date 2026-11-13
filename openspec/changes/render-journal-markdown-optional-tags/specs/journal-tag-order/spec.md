## ADDED Requirements

### Requirement: Journal tags are optional

Android SHALL permit journal creation and saving without tags when valid work content exists and server allowedActions permits the operation. The editor SHALL label tags as optional. Users SHALL be able to remove all tags from an existing journal, submitting an empty tag set and no primary tag. Existing primary-tag ordering SHALL remain unchanged when tags are selected.

#### Scenario: Create without tags
- **WHEN** a user enters work content without choosing a tag
- **THEN** publishing proceeds without a tag-required error, including when the available tag catalog is empty

#### Scenario: Clear existing tags
- **WHEN** an authorized author removes every selected tag and saves an existing journal
- **THEN** Android submits an empty tag list and no primary tag
