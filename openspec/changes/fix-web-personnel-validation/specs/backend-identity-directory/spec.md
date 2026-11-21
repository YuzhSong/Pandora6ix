## ADDED Requirements

### Requirement: Unique identity numbers within an enterprise

The backend SHALL reject creation or changes that assign an identity number already held by another person in the same enterprise. The response SHALL be a conflict without exposing the number or its owner.

#### Scenario: Concurrent creation with the same identity number
- **WHEN** two administrators submit distinct accounts with the same identity number in one enterprise
- **THEN** only one account is created and the other request receives `ID_NUMBER_TAKEN`.

#### Scenario: Edit a historical record
- **WHEN** an administrator keeps an existing identity number while editing other personnel data
- **THEN** the duplicate-number check permits the edit even if historical data contains a duplicate.

### Requirement: Unique phone numbers within an enterprise

Personnel creation and phone-number changes SHALL reject phone numbers already assigned within the enterprise. The check SHALL compare the digits and ignore formatting characters.

#### Scenario: Same number with different formatting
- **WHEN** two concurrent requests submit different employee accounts that use the same digits
- **THEN** one succeeds and the other receives `PHONE_NUMBER_TAKEN`.
