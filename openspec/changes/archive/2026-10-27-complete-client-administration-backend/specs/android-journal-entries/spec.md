## ADDED Requirements

### Requirement: Company-date journal editing and backfill UI
Android SHALL show dates for the authenticated company's today, yesterday and day before yesterday. Today's journal MAY be edited or deleted only while server allowedActions permits and the company date still matches. Existing backfilled journals SHALL be read-only, and missing yesterday/day-before journals SHALL offer one initial submission with a clear immutable-after-submit notice. Duplicate backfill conflicts SHALL NOT trigger automatic PATCH. Stale or out-of-range drafts SHALL NOT be restored into another date.

#### Scenario: Company date differs from device date
- **WHEN** device timezone differs from the company's configured timezone
- **THEN** date options and edit eligibility follow the company timezone, and server rejection remains authoritative across midnight.

#### Scenario: Backfill already submitted on another device
- **WHEN** a backfill POST receives DAILY_JOURNAL_EXISTS
- **THEN** Android refreshes the existing journal and reports the conflict without attempting to append or edit it.
