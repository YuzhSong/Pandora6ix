## ADDED Requirements
### Requirement: Generated initial personnel passwords
Single and batch personnel creation SHALL generate independent random initial passwords satisfying the backend password policy. Generated values SHALL remain stable while editing or retrying a draft and SHALL be available through the existing successful account handoff. Unused blank rows SHALL NOT create accounts. Creation forms and CSV templates SHALL omit password fields; passwords SHALL appear only in successful account delivery.
#### Scenario: Create multiple employees
- **WHEN** an administrator fills two batch rows without passwords
- **THEN** independent passwords are generated and delivered only for confirmed successful accounts
### Requirement: Business role instead of duplicate job text
Personnel creation SHALL offer 创始人, 部门老总, 团队长 and 员工 as business role choices, submit the corresponding roles, and validate organization kind and supervisor hierarchy. Personnel cards and profile editors SHALL omit redundant free text job titles and retain role badges. System administrator permission SHALL remain separate.
#### Scenario: Select a team leader
- **WHEN** an administrator selects 团队长 for a new person
- **THEN** creation requires a team organization and valid superior and submits TEAM_LEAD

#### Scenario: Download the CSV template
- **WHEN** an administrator downloads the personnel import template
- **THEN** it contains eight columns without initial passwords and imported rows receive generated passwords internally
