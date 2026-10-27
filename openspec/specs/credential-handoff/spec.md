# credential-handoff Specification

## Purpose
Define validated employee onboarding and temporary encrypted initial/reset password delivery, with company administrator access, bounded retention, fresh exports and employee password privacy.
## Requirements
### Requirement: Editable employee onboarding table
Web SHALL offer an editable bulk creation table with row addition/removal and selectors, alongside CSV file import on the same screen. Imported rows SHALL populate the editable table without a raw CSV text area. It SHALL validate before submission, ignore blank rows and limit batches to 100 employees. The modal SHALL prevent background scrolling and duplicate submission.

#### Scenario: Fill and submit employee rows
- **WHEN** an administrator completes table rows and submits the batch
- **THEN** invalid input prevents all creation with actionable errors, while valid rows use the existing account creation API and expose successful initial credentials for delivery

#### Scenario: Correct input and retain results
- **WHEN** input has missing profiles, invalid passwords, duplicate accounts or invalid reporting assignments
- **THEN** the administrator can correct the indicated rows before creation, and after submission each row retains its result alongside successful credential delivery

### Requirement: Durable temporary credential delivery
The system SHALL retain encrypted initial and administrator-reset employee passwords for a configurable delivery window, default seven days. An authorized administrator SHALL retrieve and batch-export available credentials across page reloads and server restarts. Missing encryption configuration SHALL fail creation/reset atomically rather than lose delivery records. Ordinary directory and profile responses SHALL contain no passwords.

#### Scenario: Export after re-login
- **WHEN** a new employee was successfully created and the administrator reloads or signs in again within the delivery window
- **THEN** the server returns the employee's current account and temporary password for delivery

### Requirement: Employee password privacy
Employee self-service password changes SHALL atomically erase retained handoff secrets. Subsequent administrator reads and exports SHALL NOT return any password for that employee. External hash changes, expiry, disabled accounts and ADMIN roles SHALL also prevent handoff. Historical accounts without retained credentials SHALL remain unavailable without modifying their passwords.

#### Scenario: Employee changes password
- **WHEN** an employee changes their password using the existing password endpoint
- **THEN** later administrator exports omit the password and show changed status, even if the new password text equals the initial password

#### Scenario: Administrator resets password
- **WHEN** an administrator successfully resets an eligible employee password
- **THEN** the replacement temporary password supersedes the previous handoff and is available for delivery until employee change or expiry

### Requirement: Restricted retrieval and concise interface
Handoff endpoints SHALL require company-scoped administrator authorization and disable HTTP caching. Web SHALL fetch fresh credentials for copy/download, discard secrets on close, session loss or retrieval failure, and poll visible dialogs to refresh status. It SHALL show concise status labels without explanatory paragraphs, manual password registration or persistent browser password storage.

#### Scenario: Revocation after opening export
- **WHEN** an employee changes password after the administrator opened the export dialog
- **THEN** the next copy/download retrieves fresh server data and excludes that employee's password

#### Scenario: Wrong company or role
- **WHEN** a non-administrator or another company's administrator requests an employee's handoff
- **THEN** the request is rejected without disclosing credentials

### Requirement: Excel handoff and password column
The handoff table SHALL display the plaintext temporary password only for AVAILABLE employees and a dash otherwise. Download SHALL produce a genuine .xlsx workbook containing name, company login identifier, account and temporary password as text cells. Employee-selected passwords SHALL never enter the workbook. Copy SHALL retain its existing plain-text format.

#### Scenario: Deliver readable credentials in Excel
- **WHEN** an administrator downloads the current handoff list
- **THEN** Excel can open the workbook, leading zeros and password characters are preserved, and an available employee can log in with the exact exported password
