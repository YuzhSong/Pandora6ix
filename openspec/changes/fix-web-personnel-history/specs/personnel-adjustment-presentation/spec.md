# Spec Delta

## Purpose
Provide explicit organization transfers, readable personnel selection and complete historical context while preserving immutable audit facts and one semantic audit per lifecycle operation.

## ADDED Requirements

### Requirement: Explicit transfer and contextual history
Web SHALL offer employees an explicit option to follow the selected supervisor's organization, show organization paths in successor options, and display full before/after personnel context. Missing historical names SHALL use separately marked current reference labels or original identifiers without changing captured snapshots.

#### Scenario: Transfer employee
- **WHEN** an employee's supervisor is changed with organization following enabled
- **THEN** the visible organization and submitted organization follow that supervisor, with manual organization selection still available

#### Scenario: Read legacy history
- **WHEN** organization or supervisor names were not captured
- **THEN** Web shows clearly marked current references or original IDs and never presents them as captured historical names

### Requirement: Single semantic lifecycle audit
A successful job adjustment or departure SHALL write one semantic operation record, without an extra account update record from reused validation logic.

#### Scenario: Save job adjustment
- **WHEN** an administrator saves a valid job adjustment
- **THEN** one JOB_CHANGE log is committed and no nested account UPDATE log is added

### Requirement: Consistent supervisor organization selection
Ordinary employees SHALL inherit the selected supervisor organization when the supervisor is selected in personnel creation, profile editing and job changes. Explicit organization selection afterwards SHALL remain possible. Batch creation with an omitted organization SHALL use the supervisor organization. Manager roles SHALL retain explicit role-compatible organization selection.

#### Scenario: Create without selecting a department
- **WHEN** an administrator selects an employee supervisor without selecting an organization
- **THEN** the form and submitted data use that supervisor organization and the returned employee appears in that organization group

#### Scenario: Change a supervisor after selecting an organization
- **WHEN** an ordinary employee selects a new supervisor
- **THEN** the visible organization follows the new supervisor; a subsequent explicit organization selection can override it

### Requirement: Automatic single-person account creation and concise forms
New single-person drafts SHALL receive an automatically generated login account without requiring manual input. The account SHALL follow name edits and remain stable during unrelated edits and avoid known case-insensitive account collisions. Existing account editing SHALL preserve the original account. Personnel screens SHALL omit the added explanatory paragraphs and changed-field suffixes requested for removal.

#### Scenario: Open and edit a new person draft
- **WHEN** an administrator opens a new person form and edits the name or organization
- **THEN** a login account is generated from the entered name and remains unchanged during organization edits

#### Scenario: Review personnel history
- **WHEN** personnel history is opened
- **THEN** the complete before-and-after table remains visible with plain field labels and without the added explanatory paragraph

### Requirement: Mobile-ready initial credential delivery
New generated passwords SHALL contain twelve cryptographically random digits. Delivery SHALL preserve the actual password verbatim and include separate enterprise domain and login name alongside the combined account for mobile login. Existing credentials SHALL only change through the password-reset command.

#### Scenario: Deliver to mobile employee
- **WHEN** an administrator copies or exports a newly generated credential
- **THEN** the password matches the creation request and the employee receives separate enterprise domain and login name fields

### Requirement: Name-based default accounts and numeric passwords
New personnel accounts SHALL default to lowercase name pinyin without tone marks and append numeric suffixes on collision. Newly generated initial passwords SHALL contain exactly twelve random digits. Existing accounts and credentials SHALL remain unchanged.

#### Scenario: Name entry and duplicate names
- **WHEN** a name is entered for a new person or a bulk row has no explicit account
- **THEN** its default account uses name pinyin with a numeric suffix if necessary and its generated password contains digits only
