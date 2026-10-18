## ADDED Requirements

### Requirement: Empty organization deletion
The backend SHALL allow ADMIN to delete an empty department or team in their own enterprise using DELETE /admin/org-units/{id} with a required nonnegative expectedVersion JSON field. Success SHALL return 204. Company roots, units with children and units with assigned users including disabled accounts SHALL be preserved. Concurrent directory writes SHALL preserve these constraints.

#### Scenario: Empty leaf deleted
- **WHEN** an administrator deletes an empty unit with its current version
- **THEN** it disappears from directory reads and a repeated delete returns 404

#### Scenario: Protected or occupied unit
- **WHEN** deletion targets the root, a unit with children or a unit with assigned accounts
- **THEN** the server returns 409 with a specific blocking code and preserves all records

#### Scenario: Version and authority boundaries
- **WHEN** deletion has a stale version, a foreign enterprise identifier, a non-admin actor or no authentication
- **THEN** it returns 409, 404, 403 or 401 respectively without removing the unit
