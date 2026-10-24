# Proposal

## Why

Android currently creates system notifications only while the application is polling in the foreground. Approval requests and decisions therefore do not reliably reach users after they leave the app. This change adds account-bound JPush delivery backed by the existing durable in-app notification records.

## What Changes

- Register and deactivate JPush installations through authenticated backend endpoints, always deriving the account and enterprise from the session.
- Queue push deliveries transactionally with notification creation and retry them asynchronously through the JPush REST API.
- Notify assigned reviewers when a task awaits review and notify submitters when a review is approved or rejected.
- Integrate the Android JPush SDK, system-notification permission, preference switch, and notification-tap navigation.
- Keep provider credentials outside source control; push delivery remains safely disabled when server credentials or Android app configuration are absent.

## Capabilities

### New Capabilities
- `push-notification-delivery`: account-bound device registration, durable provider delivery, and Android background notifications.

### Modified Capabilities
- `app-settings`: the notification master switch controls whether this installation is registered for remote notifications.

## Impact

Backend Flyway schema, notification and task-review services, authenticated device APIs, JPush REST integration, OpenAPI, and integration tests; Android Gradle dependencies, manifest configuration, session lifecycle, notification handling, and settings; deployment requires JPush AppKey and Master Secret environment configuration.
