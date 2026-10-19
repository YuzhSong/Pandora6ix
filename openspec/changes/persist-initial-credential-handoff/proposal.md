# Proposal

## Why

The current page-only password list disappears on reload and cannot support onboarding across sessions. Administrators need to deliver initial credentials later, while employee-selected passwords must never be available to them.

## What Changes

- Persist encrypted, expiring employee initial/reset credentials separately from authentication hashes.
- Atomically erase deliverable credentials on employee password changes; export only currently valid temporary credentials, scoped to company administrators.
- Reload and batch-export credentials from the server on demand; show concise statuses, with no explanatory paragraphs or manual password registration.
- Document required external encryption key and deployment migration; never backfill guessed historical passwords or change existing cloud passwords.
- Offer an editable row-based employee creation table alongside CSV import, without introductory explanatory prose.

## Capabilities

### New Capabilities

- `credential-handoff`: Durable, restricted initial credential delivery and employee change revocation.

### Modified Capabilities

None.

## Impact

Backend Flyway migration and encrypted handoff service, directory/password controllers, Web repositories and account dialogs, OpenAPI, integration tests and deployment documentation. Android continues using the existing password-change endpoint.
