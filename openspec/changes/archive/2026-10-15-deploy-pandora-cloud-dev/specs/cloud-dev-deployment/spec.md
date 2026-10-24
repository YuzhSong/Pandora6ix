## ADDED Requirements

### Requirement: Persistent cloud application and public domain
Pandora SHALL run its backend and persistent database under /opt/Pandora and serve the Web client at https://www.rainycode.cn with a trusted certificate. Web and future Android clients SHALL share the authenticated backend, without direct public database access. Existing local database contents SHALL be migrated consistently once; release updates SHALL retain database contents and external secrets. Unrelated server workloads SHALL be preserved.

#### Scenario: Redeploy existing application
- **WHEN** a new program release is activated
- **THEN** the database and accounts remain in the shared data directory and both Web and API are reachable through the configured domain

### Requirement: Automatic verified dev release
Every remote dev push SHALL trigger a workflow that verifies and builds backend/Web, then transfers a manifest-bound release to the cloud. Failed checks SHALL prevent deployment. SSH credentials SHALL be dedicated, encrypted outside Git and constrained to deployment; host identity SHALL be pinned. Releases SHALL serialize, reject stale run numbers and expose their deployed revision for verification.

#### Scenario: Dev changes
- **WHEN** a dev update passes module checks
- **THEN** the workflow deploys its built commit and the public release marker identifies that commit

#### Scenario: Invalid payload
- **WHEN** a release archive contains path traversal, links, invalid metadata or mismatching file hashes
- **THEN** it is rejected before stopping the service or altering the active release

### Requirement: Consistent backup and failed-release recovery
Deployment SHALL stop the single backend before copying its file database, keep private backup files, switch program releases atomically and require backend health. A failed new program SHALL restore the previous program release and report failure. Database backup restoration SHALL be explicit, never an automatic overwrite of new user data.

#### Scenario: New backend cannot start
- **WHEN** a newly activated release fails health verification
- **THEN** the previous program is restored when available, the failed deployment reports failure, and the persistent database is not replaced automatically
