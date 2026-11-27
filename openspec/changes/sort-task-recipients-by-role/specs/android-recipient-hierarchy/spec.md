## ADDED Requirements

### Requirement: Recipient role order within an organization

The Android task recipient picker SHALL order each organization's direct candidates by descending business seniority using their business rank. Candidates with equal rank or no rank SHALL keep their relative input order, and candidates with no rank SHALL follow ranked candidates. The picker SHALL apply the same ordering to the current user using the user's business roles.

#### Scenario: Department and team leads come first
- **WHEN** a department or team has its lead among the displayed candidates and lower-ranked members appear earlier in the input
- **THEN** the department lead appears first among the department's direct members, and the team lead appears first among that team's members

#### Scenario: A lead does not match the search
- **WHEN** the available picker candidates include team members without their lead
- **THEN** the picker orders the available candidates in their organization without fetching or inserting the lead for sorting
