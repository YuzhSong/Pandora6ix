# Spec Delta

## Purpose

Deliver approval and task notifications to a user's registered Android installations when the app is backgrounded or closed, while preserving tenant isolation, account ownership, and the existing in-app notification record as the source of truth.

## ADDED Requirements

### Requirement: Account-bound push installations

The backend SHALL allow an authenticated user to register and deactivate a push installation. Installation ownership and enterprise scope SHALL be derived from the authenticated session, never from request-supplied user or enterprise identifiers. Re-registering the same installation SHALL update its provider registration identifier without creating duplicate active installations. Logging out SHALL deactivate all installations for that session's account.

#### Scenario: Register an installation
- **WHEN** an authenticated user submits an installation identifier and JPush registration identifier
- **THEN** the backend associates that installation with only the authenticated user's account and enterprise.

#### Scenario: Deactivate an installation
- **WHEN** a user deactivates one of their registered installations
- **THEN** only that user's installation is disabled and it receives no future pushes.

#### Scenario: Logout
- **WHEN** an authenticated user logs out
- **THEN** all push installations belonging to that account are deactivated before the session ends.

### Requirement: Durable tenant-scoped push delivery

The backend SHALL create push-delivery work in the same transaction as each recipient's in-app notification, deliver only to active installations of that recipient in the same enterprise, and retry transient provider failures without rolling back the business operation. Delivery work SHALL be deduplicated per notification and installation. Push payloads SHALL contain only the notification identifier and non-sensitive display text, never bearer credentials or private task details. Missing provider configuration SHALL leave in-app notifications functional and SHALL NOT fail the business operation.

#### Scenario: Deliver a committed notification
- **WHEN** a business transaction commits a notification for a user with active installations
- **THEN** asynchronous delivery targets only that user's active installations and references the persisted notification.

#### Scenario: Transaction rolls back
- **WHEN** the business operation creating a notification rolls back
- **THEN** no push is delivered for that notification.

#### Scenario: Provider is unavailable
- **WHEN** JPush rejects or cannot receive a delivery request
- **THEN** the delivery remains retryable and the underlying business operation remains successful.

#### Scenario: Push credentials are absent
- **WHEN** JPush credentials are not configured
- **THEN** in-app notification records continue to be created and the backend does not attempt external delivery.

### Requirement: Approval event notifications

The backend SHALL persist and push a review-request notification to the assigned reviewer when a task enters pending approval, and a review-result notification to the submitter when the request is approved or rejected. Rejected or invalid requests SHALL NOT generate committed push deliveries.

#### Scenario: Review request submitted
- **WHEN** a valid task review is committed
- **THEN** the assigned reviewer receives one persisted review-request notification and eligible registered installations receive its push.

#### Scenario: Review approved or rejected
- **WHEN** an assigned reviewer commits an approval or rejection
- **THEN** the submitter receives one persisted result notification and eligible registered installations receive its push.

#### Scenario: Review decision fails
- **WHEN** a decision fails validation or its transaction rolls back
- **THEN** no result notification or push delivery is committed.

### Requirement: Android background notification interaction

Android SHALL initialize JPush when configured, register its installation only while the signed-in user has enabled notifications and granted system notification permission, and deactivate the installation when the preference is disabled or the user logs out. A tapped push SHALL open the application and navigate to its in-app notifications. When provider configuration is absent, normal application startup and in-app notifications SHALL continue to work.

#### Scenario: Notification received while app is closed
- **WHEN** JPush delivers a notification to an enabled installation while the app is not running
- **THEN** Android displays a system notification that can launch the application when tapped.

#### Scenario: Notifications disabled
- **WHEN** the user turns off the notification master switch
- **THEN** the installation is deactivated and no new remote notifications are targeted to it.

#### Scenario: Notification permission denied
- **WHEN** Android notification permission is denied
- **THEN** the installation is not registered for remote notification delivery and the app remains usable.

#### Scenario: Push configuration absent
- **WHEN** the Android build has no JPush AppKey
- **THEN** the app starts normally and continues to use its non-push features without crashing.
