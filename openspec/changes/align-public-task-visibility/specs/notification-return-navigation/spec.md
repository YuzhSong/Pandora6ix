## MODIFIED Requirements

### Requirement: Return to originating notification list
Android SHALL restore the open notification list when returning from a task detail or approval destination opened through that list. Opening a destination SHALL NOT dismiss the originating list state. Explicitly closing the list or pressing system back while the list is visible SHALL dismiss the list. Saved UI state SHALL be isolated by account.

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

## ADDED Requirements

### Requirement: Detail visits mark linked notifications read
Android SHALL mark linked unread notifications read when opening a task detail from the home board, calendar or notification list, or an approval detail from the approval center. Android SHALL refresh the message badge and list after marking linked notifications read. The approval badge SHALL count pending approvals and pending acceptance requests for the current user.

#### Scenario: Open a task from the calendar
- **WHEN** the current user opens a task with unread notifications from the calendar
- **THEN** its notifications are marked read and the available approval and acceptance work continues to determine the approval badge

### Requirement: Approval result opens its authorized detail
Android SHALL open an approval result message at the corresponding approval detail when the user can read that approval. If the detail cannot be loaded, Android SHALL fall back to the linked task when present. Android SHALL clear the message's focus when leaving the approval center.

#### Scenario: Open the approval center after a message
- **WHEN** a user views an approval result from a message, closes the approval center and reopens it from the profile
- **THEN** the approval center opens at its list with the previous message's focus cleared
