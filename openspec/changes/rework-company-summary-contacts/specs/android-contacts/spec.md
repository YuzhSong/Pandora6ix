## ADDED Requirements

### Requirement: Contacts entry on the company page
The company page SHALL show a “通讯录” button immediately to the left of “任务汇总” in its top control row, opening the contacts destination without adding a bottom navigation tab. Back SHALL return to the company page with its date, mode and filters preserved.

#### Scenario: Open contacts
- **WHEN** a user taps 通讯录 on the company page
- **THEN** the contacts page opens, and back returns to the same calendar date and mode

### Requirement: Direct reports and organization in contacts
Contacts SHALL hold the people content formerly in the task summary: the complete list of authorized candidates marked DIRECT with avatar, name and job title, an entry to the expandable organization page, and member detail with personal information, authorized telephone, responsible tasks and recent journals. Data SHALL come only from the existing authorized candidate, task, journal and contact APIs. An account without manage capability SHALL see an explanation and SHALL NOT request candidates.

#### Scenario: Manager opens a direct report
- **WHEN** a manager taps a direct report in contacts
- **THEN** the member detail shows that member's information, telephone, responsible tasks and recent journals

#### Scenario: Partial candidate page
- **WHEN** more candidate pages exist but no DIRECT person has been loaded
- **THEN** contacts disclose the partial result and allow loading more instead of claiming there are no direct reports

### Requirement: Subordinate journals with date filter
Contacts SHALL offer a subordinate journal list (scope managed) and per-member journal lists, each with a date filter of 全部, 今天, 近 7 天, 本月 and 指定日期. The filter SHALL be applied by the server through `GET /work-logs` `from`/`to` parameters in the business timezone, restart pagination when changed, and be shown in the list header. Member lists SHALL keep their author filter.

#### Scenario: Filter by a chosen date
- **WHEN** a manager selects 指定日期 2026-11-05 in the subordinate journal list
- **THEN** only subordinate journals for 2026-11-05 are requested and listed, and choosing 全部 restores the unfiltered list

#### Scenario: Member list under a filter
- **WHEN** a member's journal list is filtered to 近 7 天 and the loaded pages contain none of that member's journals while more pages exist
- **THEN** the list offers continued paging instead of claiming the member has no journals in that range
