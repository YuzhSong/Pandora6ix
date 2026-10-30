## ADDED Requirements

### Requirement: Journal detail actual creation time
Android journal details SHALL display the date and time derived exclusively from publishedAt in the company business timezone in the creation time section.

#### Scenario: Backfilled journal
- **WHEN** a journal is created today for a previous workDate
- **THEN** creation time displays today's actual creation date and time, independent of workDate

#### Scenario: UTC date boundary
- **WHEN** publishedAt falls on a different date in the company timezone than UTC
- **THEN** both displayed date and time follow the company timezone

#### Scenario: Missing creation timestamp
- **WHEN** publishedAt is missing or cannot be parsed
- **THEN** creation time displays an unknown placeholder rather than workDate
