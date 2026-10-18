## ADDED Requirements

### Requirement: Controlled correction of the existing company directory
Authorized repair SHALL apply the reviewed pinyin mapping with version checks, retain existing user IDs and credentials, and configure songyuzheng's confirmed supervisor as Lu Chuan. It SHALL preserve other profiles, roles, organizations, statuses, reporting relations and business references. Private snapshots and credentials SHALL remain outside Git.

#### Scenario: Named account correction
- **WHEN** the administrator applies the confirmed mapping and supervisor correction
- **THEN** every mapped account has its expected name, songyuzheng references the same-team active leader, unchanged names are retained and no account or password is recreated

#### Scenario: Concurrent or mismatched identity
- **WHEN** a target no longer matches its reviewed identity or expected version
- **THEN** the repair stops instead of overwriting unrelated directory changes
