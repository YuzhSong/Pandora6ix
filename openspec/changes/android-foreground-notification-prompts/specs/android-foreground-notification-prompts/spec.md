# Android Foreground Notification Prompts

## ADDED Requirements

### Requirement: Approval notifications use a high-importance channel
The Android client MUST post approval events on a stable notification channel configured for high importance, sound, and vibration when the OS supports those settings.

#### Scenario: Background approval event
- **WHEN** an authenticated device receives an approval event while Pandora is not visible
- **THEN** Android posts a notification using the approval channel with the notification ID and event type as non-sensitive extras
- **AND** tapping it opens Pandora and refreshes the authenticated notification/review view

### Requirement: Foreground approval events are visible in the app
The Android client MUST show a dismissible foreground prompt for a newly received approval event while the main activity is resumed.

#### Scenario: Foreground approval event
- **WHEN** Pandora is visible and a new approval event arrives
- **THEN** the user sees an in-app prompt with an action to open the review center
- **AND** dismissing the prompt does not mark the durable notification as read

### Requirement: Permission and OS restrictions are respected
The Android client MUST retain durable in-app notifications when notification permission is denied or the device suppresses heads-up presentation, and MUST NOT repeatedly request denied permission.

#### Scenario: Notifications denied
- **WHEN** Android notification permission is denied
- **THEN** the event remains available in the in-app notification list
- **AND** the client does not loop permission requests
