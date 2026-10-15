## Why

The user requests the existing database, backend and Web on their cloud server under a new Pandora directory, accessible at www.rainycode.cn, with automatic deployment whenever dev changes. FactoryVision/Jenkins are no longer installed; the existing Dachuang video-monitor resources must remain untouched.

## What Changes

- Deploy the existing persisted H2 database and Java backend under /opt/Pandora with persistent data separated from immutable releases.
- Serve the built Web app and same-origin API through Nginx at www.rainycode.cn with HTTPS and certificate renewal.
- Build/test dev pushes in GitHub Actions and deploy verified artifacts using a dedicated constrained SSH key; serialize releases, back up the stopped database, health-check and roll back failed program releases.
- Keep host credentials, deployment keys, bootstrap secrets and database contents outside Git; document operation, rollback and verification.

## Capabilities

### New Capabilities
- `cloud-dev-deployment`: Shared cloud persistence, domain/TLS delivery and verified automated dev deployments.

### Modified Capabilities
None; business API behavior remains governed by existing backend changes.

## Impact

Add deployment scripts/configuration, a dev Actions workflow, operational documentation and this OpenSpec change. Install only Java 21, Nginx and certificate tooling required by Pandora; use systemd rather than restoring the removed Jenkins stack on this 2 GiB server. Preserve unrelated containers, networks and files. The user confirmed including the completed local backend changes and publishing them to dev. No Android networking implementation or new business services.
