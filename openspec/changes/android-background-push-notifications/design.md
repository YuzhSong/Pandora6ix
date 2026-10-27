# Design

## Context

The backend already persists deduplicated per-user notifications. Android currently polls task snapshots only while signed in, and the settings switch controls local reminders. There is no device-token API or external push transport.

## Goals / Non-Goals

**Goals:** Reliably deliver committed approval notifications to opted-in Android installations, preserve tenant/account boundaries, support retry, and make push taps enter the authenticated app.

**Non-Goals:** iOS/APNs, web push, vendor aggregation, guaranteed delivery while Android force-stop restrictions or OEM battery policies block JPush, and migration of historical notifications into pushes.

## Decisions

- Use JPush Android SDK and its REST v3 API. The Android AppKey is a public build configuration value; Master Secret is backend-only environment configuration and is never stored in the repository or database.
- Bind registrations to the authenticated principal. Android supplies an installation UUID and provider registration ID only; the backend derives user and enterprise. Re-registration updates the same account/installation row, while logout and the settings switch deactivate rows.
- Add a delivery outbox tied to persisted notification rows. Notification creation and per-active-device delivery rows are in one business transaction. A scheduled worker sends after commit and records attempts/backoff, avoiding provider calls inside business transactions.
- Keep push display text generic (for example, "有新的审批申请" or "审批结果已更新"). Include only notification ID and kind in extras; the app loads protected details through existing authenticated APIs after launch.
- Add review-request and review-result event types while preserving the existing notification kind vocabulary for older clients. A request is created with the task/review transaction; a result is created with the accepted decision transaction.
- If provider credentials are absent, keep notification persistence operational and skip remote send attempts. Android builds without an AppKey skip JPush initialization and registration.

## Risks / Trade-offs

- [JPush or a carrier/OEM may delay or suppress delivery] → Keep durable in-app notifications as source of truth and retry transient REST failures; document force-stop and battery-policy limits.
- [Provider retries can duplicate a visible push after an ambiguous timeout] → Use stable notification identifiers and per-notification/device outbox keys; client notification IDs replace duplicate display where possible.
- [Registration identifiers can rotate] → Re-register on app startup and after token refresh; upsert by installation ID.
- [Production credentials are unavailable in local development] → Use environment variables and a disabled-provider mode; validate payloads with tests and require deployment secrets for live delivery.

## Migration Plan

Additive Flyway migration creates push-device and push-delivery tables and expands the notification kind constraint for review events. Deploy backend first with JPush credentials, then release Android with the matching AppKey. Older Android versions continue to use existing local behavior. Rollback can disable provider configuration and leave persisted notifications intact; do not delete delivery history automatically.

## Open Questions

None; JPush is the confirmed provider. Production AppKey and Master Secret are deployment inputs, not design decisions.

## Integration review, 2026-10-27

Retain the notification preference and one-time permission request from PR #28. Registration additionally requires the Android system grant and configured SDK; denial does not erase local reminder preferences. SDK registration callbacks and session-token changes trigger registration refresh; transient failures retry. Notification taps are forwarded explicitly through the custom JPush receiver.

The worker checks active account and installation ownership when selecting, claiming and preparing to send; attempts fence stale workers and stop at five. Same-owner re-registration preserves queued work, while ownership changes cancel it. A provider-accepted request cannot be recalled if account ownership changes during network I/O. Notifications are generic and protected details remain server-authenticated.

The original credential-bearing Word guide was removed from candidate history and replaced with a sanitized Markdown guide. Old credentials require rotation; external-provider and physical-device acceptance remain pending.
