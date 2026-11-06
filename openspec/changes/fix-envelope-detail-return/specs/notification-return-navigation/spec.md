## MODIFIED Requirements

### Requirement: Return to originating notification list
Android SHALL restore the open notification list when returning from a task detail or approval destination opened through that list. Opening a destination SHALL NOT dismiss the originating list state. Explicitly closing the list or pressing system back while the list is visible SHALL dismiss the list. Selecting any bottom navigation item SHALL dismiss the list so returning to Home through bottom navigation SHALL NOT reopen it. Push arrival and ordinary task entry SHALL NOT open a closed list. Existing read marking and destination resolution SHALL remain unchanged. Saved UI state SHALL be isolated by account.

#### Scenario: Read multiple task messages
- **WHEN** a user opens a task message from the envelope and returns from task detail
- **THEN** the notification list is visible and another message can be opened without tapping the envelope again.

#### Scenario: Review message return
- **WHEN** a pending review message opens the approval center and the user returns
- **THEN** the originating notification list is visible.

#### Scenario: Explicit close
- **WHEN** the user closes the notification list using its close button, backdrop or system back
- **THEN** the home screen is shown without the notification list.

#### Scenario: Ordinary task entry
- **WHEN** a task is opened from the calendar with no notification list open
- **THEN** returning shows the calendar without opening notifications.

#### Scenario: Bottom navigation dismisses message context
- **WHEN** a user leaves the notification list or its destination using bottom navigation and then selects Home
- **THEN** Home is shown with the notification list closed.

#### Scenario: Push and account changes
- **WHEN** a push arrives while the list is closed, or a different account becomes active
- **THEN** the notification list remains closed.
