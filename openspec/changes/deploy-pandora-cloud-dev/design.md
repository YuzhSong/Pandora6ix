## Context

The supplied SSH account reaches Ubuntu 24.04 with passwordless sudo, 1.9 GiB RAM and 23 GiB free disk. www.rainycode.cn resolves to that SSH host. Ports 80/443 are unused. FactoryVision/Jenkins are absent; three stopped video-monitor containers and /opt/video-monitor belong to Dachuang and are preserved. The repository is public and GitHub Actions is enabled. Local backend/data/pandora.mv.db exists and the backend is not listening locally.

## Goals / Non-Goals

**Goals:** Deploy Web/backend and existing database on the cloud host; trusted HTTPS domain; one tested pipeline triggered by every dev push; persistent data and release rollback.

**Non-Goals:** Restore Jenkins, touch video monitoring, add MySQL/PostgreSQL compatibility without migration tests, Android network integration, new business features or publishing credentials.

## Decisions

- Use /opt/Pandora/releases/<sha>-<run>, current symlink, shared/data, backups and root-owned config. H2 remains embedded in one Java 21 backend process; migrate a consistent local snapshot once, never overwrite it on redeploy. This satisfies shared Web/Android access through the same API without exposing database ports.
- Run backend as a dedicated unprivileged pandora-app systemd service on 127.0.0.1:18080 with bounded heap. Nginx serves current/web, proxies /api/ and /backend/ to loopback, and exposes a release marker for deployment checks. Redirect HTTP to HTTPS after Certbot issuance; retain ACME renewal.
- Build on hosted GitHub runners to avoid Maven/Node memory pressure on the small server. Every push to dev runs backend tests/package, Web tests/build and OpenAPI drift checks. Pin official setup actions by verified commit. Use read-only repository permission and optional manual dev runs.
- Separate the built application classes from versioned dependency jars; cache dependencies by SHA-256 and probe missing hashes through the same forced command. Cross-border full-bundle SSH upload proved too slow, so routine dev releases transfer only application/Web changes and missing dependencies. Link each release to its exact verified dependencies, not a wildcard of all historical cache versions.
- Generate a dedicated SSH key outside Git, store its private half in encrypted GitHub Actions secrets, and pin the server host key. A locked pandora-deploy account has one forced, restricted root-owned receive-release command; no general shell/forwarding. The receiver validates a bounded archive, safe paths, immutable manifest and file hashes before changing service state.
- Serialize releases using flock; reject older run numbers. Stop the backend, back up the database consistently, atomically switch the release, restart and wait for health. Restore the previous program symlink on failure; never automatically restore an old database over live user writes. Keep backups private and unrelated resources unchanged.
- Receive and validate uploads before taking the activation lock, with a bounded timeout and signal cleanup. A broken uploader must not hold up a healthy later workflow. Use distinct run attempts to permit safe workflow reruns.
- Initial database transfer and SSH credential handling use existing local secure source files via in-memory tools. Never add sshkey.md, database contents, environment files or private keys to commits or CI logs.

## Risks / Trade-offs

- Brief deployment downtime while H2 is stopped for consistent backup → build off-host, keep restart bounded and verify rollback.
- Cloud firewall or ACME validation may block public ports → validate externally and report any provider-side blocker; do not claim TLS success before a trusted public handshake.
- Future destructive schema migrations may prevent old binaries from using new data → preserve backup and report rollback limits rather than silently replace the database.
- Existing development data may not include suitable real credentials → inspect only counts/schema and preserve accounts; do not create or reset passwords unless needed for requested first access.

## Migration Plan

Complete and validate deployment artifacts, install isolated service/domain configuration, transfer the offline database snapshot, configure the constrained deployment credential and GitHub secrets, publish only the user-selected first-release scope to dev, then observe a real push-triggered Actions deployment. Verify public Web/API, persistence and certificate renewal. Changes remain restricted to Pandora paths and necessary shared Nginx/OS services.
