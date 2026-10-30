# Proposal

## Why

Approval notifications currently arrive only in the notification shade. Users can miss time-sensitive approvals because the app does not provide a visible heads-up prompt while the app is open or immediately after a background delivery.

## What Changes

- Create a high-importance Android notification channel for approval events with sound, vibration, and heads-up presentation when supported by the device.
- Show an in-app approval prompt while Pandora is in the foreground, with actions to open the notification/review center or dismiss it.
- Preserve notification-tap routing and the durable in-app notification list as the source of truth.
- Keep behavior graceful when notification permission is denied or the device/vendor suppresses heads-up notifications.

## Capabilities

### New Capabilities

- `android-foreground-notification-prompts`: Visible foreground approval prompts and high-importance Android notification presentation.

### Modified Capabilities

None.

## Impact

- Android notification initialization, JPush receiver/service integration, foreground app state, and notification UI.
- No backend API or database changes.
- Android tests and debug build validation.
