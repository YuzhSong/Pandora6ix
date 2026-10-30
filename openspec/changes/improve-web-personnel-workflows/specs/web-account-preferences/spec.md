## ADDED Requirements
### Requirement: Unified Web login identity display
Personnel login information, copied credentials and account delivery spreadsheets SHALL display the full account identifier as loginName@enterpriseDomain without a separate company identifier field. Company identity SHALL remain in the API model for account resolution. Missing legacy domains SHALL NOT be guessed or replaced with a hard-coded company.
#### Scenario: Deliver an existing company account
- **WHEN** an account has loginName admin and enterpriseDomain pandora
- **THEN** Web displays and copies admin@pandora, and Excel contains name, full login account and initial password without a separate company identifier column

### Requirement: Persisted Web administrator avatar
Web SHALL allow the current administrator to preview and save a validated compressed image using the existing avatar endpoint, reload the saved image with authentication after refresh, fall back to the name on missing image and report actual save failures. Input files SHALL be limited to supported PNG/JPEG/WebP images up to 5 MB and output data URLs SHALL fit the backend size limit.
#### Scenario: Save and refresh avatar
- **WHEN** the administrator selects a valid image and saves successfully
- **THEN** the persisted image appears in the header and account page and is loaded from the server after refresh
#### Scenario: Invalid or failed upload
- **WHEN** the image is invalid or the server rejects the save
- **THEN** Web shows an error, does not report success and preserves the previous saved avatar
