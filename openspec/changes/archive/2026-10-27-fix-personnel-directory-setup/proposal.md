## Why

The deployed company directory still contains demo login names and songyuzheng has no explicit supervisor, so task creation fails despite an existing team leader. Personnel forms also expose redundant implementation explanations and an empty credential-export dialog.

## What Changes

- Apply the existing 31-person pinyin mapping through versioned ADMIN updates, preserving identity, credentials, roles and business references.
- Set songyuzheng's explicit supervisor to the active leader of his backend team, 陆川, after identity and role verification.
- Remove redundant personnel-edit explanations and the missing-password name list. Enable bulk handoff only when the current session has deliverable credentials.
- Record Git provenance and actual local/cloud verification without committing private personnel snapshots.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `backend-identity-directory`: controlled correction of existing company names and the specifically requested supervisor relationship.
- `organization-directory`: concise personnel editing and usable current-session credential handoff.

## Impact

Existing company records through existing ADMIN APIs; Web AdminEditor, AdminPages and CredentialTools. No new endpoint, table, credential storage, dependency or infrastructure service.
