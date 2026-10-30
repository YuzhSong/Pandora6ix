# android-journal-entries Specification

## Purpose
Define company-timezone journal creation, creation-day edit/delete eligibility and safe backfill interactions in the Android client.
## Requirements
### Requirement: Company-date journal editing and backfill UI
Android SHALL offer new journals for the authenticated company's today, yesterday and day before yesterday. Edit and delete eligibility SHALL follow server allowedActions based on the creation timestamp publishedAt in the company business timezone, not workDate: journals created today, including yesterday/day-before backfills, remain editable and deletable today and become read-only after their creation day. The client SHALL NOT show an immutable-after-submit notice for a backfill created today. Duplicate backfill conflicts SHALL NOT trigger automatic PATCH. Stale or out-of-range drafts SHALL NOT be restored into another date.

#### Scenario: Company date differs from device date
- **WHEN** device timezone differs from the company's configured timezone
- **THEN** creation date options and the company-local date of publishedAt follow the company timezone, and server allowedActions and rejection remain authoritative across midnight.

#### Scenario: Backfill already submitted on another device
- **WHEN** a backfill POST receives DAILY_JOURNAL_EXISTS
- **THEN** Android refreshes the existing journal and reports the conflict without attempting to append or edit it.
