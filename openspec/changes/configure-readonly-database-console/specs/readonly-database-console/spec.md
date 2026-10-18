## ADDED Requirements

### Requirement: Private live browser access
The operations console SHALL run against the actual backend H2 file in the backend JVM, bind only 127.0.0.1:18082, and remain absent from public Nginx routes. Access from the user's computer SHALL use a host-key-pinned dedicated SSH tunnel limited to that destination. No shell, PTY, agent or remote forwarding SHALL be granted to its SSH account.

#### Scenario: Open the configured page
- **WHEN** the user starts the provided local tunnel and opens its localhost URL
- **THEN** H2 Console is reachable and can query current committed business data, while direct public access remains unavailable.

#### Scenario: Desktop launch after the previous tunnel has stopped
- **WHEN** the user opens the desktop shortcut with no live tunnel
- **THEN** the launcher uses the Windows-installed PowerShell host, creates the tunnel and opens the authenticated page; a launch failure remains visible through an error message and a private local diagnostic log rather than silently disappearing.

### Requirement: Database-enforced read-only business views
PANDORA_VIEWER SHALL be non-admin and receive SELECT only on admin-owned PANDORA_VIEW business views. Authentication/session/initialization/idempotency records and password hashes SHALL NOT be exposed. Business writes, grants and DDL SHALL be rejected. Real data and original business schema SHALL remain unchanged by setup.

#### Scenario: Read and attempted write
- **WHEN** the reader selects a business view and then attempts an underlying-table update or schema change
- **THEN** SELECT succeeds, mutations fail, and original records stay unchanged.

#### Scenario: No blank administrator login
- **WHEN** the console is enabled against a database whose application admin previously had an empty password
- **THEN** a random externally stored admin password protects that account and the backend continues authenticating successfully.

### Requirement: Compatible lifecycle and private credentials
Setup SHALL create a consistent private backup before changing database security or restarting. The console SHALL stop with the backend so existing deploy backup semantics remain valid. Credentials and private keys SHALL remain outside Git and command-line arguments. The local tunnel SHALL have a documented start/stop path and private access notes.

#### Scenario: Normal restart or redeploy
- **WHEN** the backend stops and restarts using the existing release deployment mechanism
- **THEN** both application and console connections close before backup, and the private console returns after startup without changing the reported application release SHA.
