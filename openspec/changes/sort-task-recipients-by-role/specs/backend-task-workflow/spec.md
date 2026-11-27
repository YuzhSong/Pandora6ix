## ADDED Requirements

### Requirement: Recipient candidates include business rank

The recipient candidate response SHALL include `businessRank` based on the candidate's highest current business role. Ranks 0, 1, 2 and 3 SHALL represent founder, department manager, team lead and employee respectively. Scope, search and pagination SHALL continue to use the existing rules.

#### Scenario: Candidate has a leadership role and a legacy employee role
- **WHEN** an authorized candidate has both the team lead and employee roles
- **THEN** the response reports rank 2 for that candidate
