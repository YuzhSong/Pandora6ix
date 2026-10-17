## Context

Current cloud dev is fff4e6e, H2 2.4.240 file /opt/Pandora/shared/data/pandora, backend bound to loopback 18080. The database administrator currently has an empty password. User authorized a real read-only browser view over an SSH tunnel.

## Goals / Non-Goals

Goals: live data, SQL SELECT and table browsing, database-enforced read-only access, private transport, usable local shortcuts, unchanged release artifacts and backup lifecycle.

Non-Goals: business UI/API features, public database console, data edits, database engine migration, unrelated services.

## Decisions

- Use an optional operations launcher, compiled against the exact existing H2 jar, installed outside immutable releases in /opt/Pandora/tools/db-console. It calls the normal application main and then starts H2's official WebServer in that same JVM. This preserves the single-process embedded file lock and shutdown-before-backup guarantee. A separate console directly opening the file cannot share its lock; AUTO_SERVER would add a TCP database mode and require coordinating two processes during backups, so neither is used.
- systemd drop-in adds only external environment and the launcher to the existing classpath. h2.bindAddress=127.0.0.1 and WebServer allowOthers=false bind 18082 privately. Public Nginx remains untouched. A console failure is logged without secrets and does not terminate an otherwise healthy application; installation verifies the console and rolls back the launcher configuration if necessary.
- Initialize PANDORA_VIEWER as a non-admin. The admin-owned PANDORA_VIEW schema exposes an explicit allowlist of business tables as views, excluding credentials and sensitive authentication/idempotency tables. APP_USER's password_hash is excluded. SELECT is granted only on the views; startup refreshes view projections after application migrations. This user cannot change underlying business tables or DDL.
- Before first enablement, stop backend, take a consistent private backup, set a random nonempty SA password, update private backend.env and provision reader credentials. Never pass passwords on process command lines or write them to Git/logs. Keep secrets in root-owned server files and the user's local Pandora directory. A disabled console can still use the stronger application DB password.
- Create pandora-db-view SSH key account, limited to local forwarding to 127.0.0.1:18082; no shell/PTY/agent/remote forwarding. Pin existing host key. Local PowerShell opens a hidden ssh tunnel with PID tracking and an optional browser; stop terminates only its own verified process.

## Risks / Trade-offs

- Requires a brief backend restart and new database grants; take a backup first and verify account/data counts, application login and current release marker afterward.
- Operational launcher depends on the public application main name and H2's supported API. Test against the deployed jar, document disable procedure, and preserve it across normal releases through a separate systemd drop-in.
- Views can depend on original table columns. Refresh after additive migrations; future destructive migrations must review their dependencies. No business table rows are changed by this setup.

## Migration Plan

Test in an isolated file H2 with real backend and HTTP console login. Stage root-owned sources/classes, key/account, private environment and systemd drop-in. Stop backend, back up, initialize account/views/password, write updated environment, start backend with optional launcher. Validate local bind, live SELECT, write/DDL denial, application login, tunnel restrictions and a second service restart. Roll back by removing only the console drop-in and restarting the unchanged application with its current valid DB credentials; never replace new business data with an old backup automatically.
