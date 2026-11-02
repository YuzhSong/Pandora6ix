# android-journal-entries Specification

## Purpose
Define company-timezone journal creation, creation-day edit/delete eligibility and safe backfill interactions in the Android client.
## Requirements
### Requirement: Company-date journal editing and backfill UI
Android SHALL offer new journals for the authenticated company's today, yesterday and day before yesterday. Edit and delete eligibility SHALL follow server allowedActions for the continuous 24-hour window from publishedAt, regardless of workDate or company midnight. Android SHALL apply the three-day date range only to new journals, allow existing journals outside that range to be edited when allowedActions permits, and explain that editing/deleting expires 24 hours after creation. Duplicate backfill conflicts SHALL NOT trigger automatic PATCH. Stale or out-of-range drafts SHALL NOT be restored into another date.

#### Scenario: Company date differs from device date
- **WHEN** device timezone differs from the company's configured timezone
- **THEN** creation date options follow the company timezone, while server allowedActions and rejection remain authoritative for the elapsed 24-hour edit/delete window across midnight.

#### Scenario: Backfill already submitted on another device
- **WHEN** a backfill POST receives DAILY_JOURNAL_EXISTS
- **THEN** Android refreshes the existing journal and reports the conflict without attempting to append or edit it.

#### Scenario: Backfill date leaves creation range
- **WHEN** a journal for the day before yesterday is still within its 24-hour window on the following day
- **THEN** Android allows editing and saving it using allowedActions despite workDate being outside the three-day creation range.
