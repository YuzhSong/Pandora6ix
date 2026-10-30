## Why

Cloud task creation repeatedly fails with H2 23514 after physical JDBC connections retire. H2 2.4.240 retains the originating session while evaluating IN-based CHECK constraints; a two-connection local reproduction fails despite valid values. Short-lived integration tests did not cover retired connections.

## What Changes

- Pin the backend to the released H2 2.5.252 containing the upstream session-lifetime fix.
- Add connection-retirement regression and file-database reopen tests, with task creation coverage for both task kinds and schedule modes after pool eviction.
- Extract real constraint identifiers from sanitized errors instead of reporting the word invalid.
- Validate existing file format and deployment packaging without changing business tables, APIs or credentials.

## Capabilities

### New Capabilities

### Modified Capabilities

- `backend-foundation`: constraint diagnostics and persistence writes remain usable across connection replacement.

## Impact

Backend H2 runtime dependency, JDBC/API regression tests, diagnostic logging and release dependency packaging. Existing cloud data must be backed up before upgrade; rollback uses the matching database backup if an older engine cannot open a file written by the new engine. Web and Android request contracts remain compatible.
