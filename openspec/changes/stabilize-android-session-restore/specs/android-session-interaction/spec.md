## ADDED Requirements

### Requirement: Stable startup session restoration
Android SHALL show one restoring-session state until the current session has been checked. A timeout, network error, server failure or malformed response SHALL preserve the stored session and show a stable retry state rather than the login form. Only an absent session or confirmed invalidated session SHALL show login. Cancellation SHALL propagate without publishing signed-out or successful state. Results from a superseded token SHALL NOT replace the current account or bind its owner. A queued session-expiration callback SHALL NOT sign out a subsequently authenticated session. Retry SHALL be explicit and SHALL NOT create an automatic loading/login loop. Diagnostic logs SHALL NOT contain tokens, passwords or response bodies.

#### Scenario: Temporary startup failure
- **WHEN** restoring an existing session encounters a network or server error
- **THEN** a retry state is shown with the stored session retained and no login form shown

#### Scenario: Profile request stalls
- **WHEN** the startup profile request stalls without a response
- **THEN** a 15-second total HTTP timeout ends the request and shows the retry state instead of an indefinite loading screen

#### Scenario: Session is actually expired
- **WHEN** the current authenticated request receives 401 and the API invalidates that session
- **THEN** the login form is shown once without automatic reauthentication

#### Scenario: Restore is canceled
- **WHEN** the restore coroutine is canceled
- **THEN** it does not publish a user or signed-out result

#### Scenario: New login replaces an old request
- **WHEN** the token changes before a restore result or queued expiration callback is delivered
- **THEN** the old result cannot bind the owner or sign out the replacement login

#### Scenario: Retry succeeds
- **WHEN** a user retries after connectivity recovers
- **THEN** the current session is restored and the home screen is shown without requesting the password again

#### Scenario: Theme content changes after authentication
- **WHEN** account-specific theme content is recomposed or replaced after session restoration succeeds
- **THEN** application-level authentication remains ready and does not enter startup loading or request another restore for that same authenticated account
