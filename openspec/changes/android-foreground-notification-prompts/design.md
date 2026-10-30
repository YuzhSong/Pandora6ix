# Design

## Context

JPush delivery and durable in-app notifications already exist. Android currently relies on the system shade and does not consistently surface an approval event while the app is visible.

## Decisions

- Create one stable `approval_events` notification channel with `IMPORTANCE_HIGH`; do not recreate or downgrade it on every launch.
- Use the existing notification ID and event type as the intent payload. Tapping a system notification opens Pandora and refreshes the protected notification list.
- Track foreground activity state in the application activity. When a new approval notification arrives while the activity is resumed, show a Compose dialog/banner with a clear action to open the review center.
- Do not display sensitive task details in the system notification or foreground prompt. Use generic approval text and load details after authentication.
- If permission is denied, retain the in-app notification and avoid repeated permission prompts. Vendor-specific suppression remains documented as an OS limitation.

## Risks

- Android users can manually lower channel importance; the app cannot raise it without user action. The settings screen should link users to notification settings when disabled.
- A foreground prompt may duplicate a system heads-up notification. The app suppresses its prompt for the same notification ID after the user has tapped or dismissed it.
