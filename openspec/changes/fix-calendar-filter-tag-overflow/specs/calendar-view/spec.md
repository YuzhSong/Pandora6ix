## ADDED Requirements

### Requirement: Single-line selected tag summary
The Android calendar SHALL display selected filter tags in a single horizontally scrollable row in every calendar mode. Tag labels SHALL NOT wrap. The prefix and clear action SHALL remain visible outside the scrolling tag region. Increasing the selected tag count SHALL NOT increase summary height or overlap calendar content. Existing AND filtering SHALL remain unchanged.

#### Scenario: Select all tags
- **WHEN** the user confirms enough selected tags to exceed the available width
- **THEN** the tags occupy one line and horizontal scrolling reveals every selected tag, while calendar content remains below the summary

#### Scenario: Long tag label
- **WHEN** a selected tag name exceeds the visible tag viewport width
- **THEN** its label remains on one line and horizontal scrolling reveals its full text

#### Scenario: Clear after scrolling
- **WHEN** the user scrolls the selected tags and taps the fixed clear action
- **THEN** all filters are cleared, the summary disappears, and unfiltered calendar content is displayed

#### Scenario: Tag scrolling preserves calendar navigation
- **WHEN** the user swipes horizontally over the selected tag summary in day, week, month, or year mode
- **THEN** only the tag strip scrolls and the current calendar period remains unchanged
