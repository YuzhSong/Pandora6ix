## ADDED Requirements

### Requirement: Server-side DeepSeek credential
The backend SHALL call DeepSeek only from the server, reading the API key, base URL and model from `pandora.deepseek.*` configuration bound to environment variables. The key SHALL NOT appear in source control, Android builds, API responses, operation logs or application logs. When the key is absent the AI endpoints SHALL remain available and return fallback content.

#### Scenario: Key not configured
- **WHEN** `PANDORA_DEEPSEEK_API_KEY` is empty and a user requests any AI feature
- **THEN** the response succeeds with deterministic fallback content and `aiGenerated=false`, and no outbound request is made.

#### Scenario: Request logging
- **WHEN** a DeepSeek call succeeds or fails
- **THEN** logs record only the feature, duration and status class, never the key, prompt text or generated content.

### Requirement: Authenticated business access
Every `/api/v1/ai/*` endpoint SHALL require an authenticated business user of an enterprise, consistent with existing business endpoints, and SHALL be read-only with respect to business data.

#### Scenario: Anonymous request
- **WHEN** a request without a valid session calls an AI endpoint
- **THEN** the backend returns 401 using the existing error contract.

#### Scenario: No business mutation
- **WHEN** any AI feature is used
- **THEN** no journal, task, tag, notification or dashboard record is created or modified.

### Requirement: Structured and validated AI output
For each feature the backend SHALL use a versioned system prompt template, request JSON output, and validate the returned JSON against that feature's required fields and length limits before returning it. Invalid, empty, timed-out or failed responses SHALL be retried at most once and then replaced by deterministic fallback content.

#### Scenario: Malformed model output
- **WHEN** DeepSeek returns text that is not valid JSON or lacks a required field twice
- **THEN** the endpoint returns fallback content for that feature with `aiGenerated=false` instead of an error.

#### Scenario: Timeout
- **WHEN** DeepSeek does not respond within the configured timeout
- **THEN** the endpoint returns fallback content within the overall request budget of 40 seconds; a retry is attempted only while at least 5 seconds of budget remain.

### Requirement: Content guardrails
Prompts SHALL instruct the model to keep a light, positive tone, base work analysis only on the supplied digest, avoid inventing facts, and not give medical, legal, financial-investment or personnel-evaluation judgments. Every response SHALL carry a disclaimer that the content is for entertainment and reference only and is not an evaluation or prediction.

#### Scenario: Disclaimer present
- **WHEN** any AI feature returns content, generated or fallback
- **THEN** the payload includes the disclaimer text displayed by the client.

### Requirement: Per-user daily quota
The backend SHALL limit DeepSeek-backed generations per user per business day (default 30, configurable). Requests beyond the quota SHALL return fallback content with a quota flag rather than calling DeepSeek.

#### Scenario: Quota exhausted
- **WHEN** a user exceeds the daily quota
- **THEN** further requests that day return fallback content with `quotaExceeded=true`.

### Requirement: Daily fortune
`GET /api/v1/ai/fortune` SHALL return today's fortune for the current user and business date: star rating, lucky color, keyword and lucky action derived deterministically from user id, date and an optional zodiac sign, plus an AI message. Once an AI-generated fortune exists for the user, date and zodiac, repeated calls that day SHALL return it without another model call while the backend process runs; fallback results are not cached so a later call may still obtain an AI message. Unknown zodiac values SHALL return 422. The zodiac sign SHALL NOT be persisted.

#### Scenario: Same day repeat
- **WHEN** a user opens today's fortune twice
- **THEN** both responses show the same stars, color, keyword, action and message, and DeepSeek is called once.

### Requirement: Workplace personality test
The backend SHALL serve an original situational question bank and score submitted answers deterministically into one original workplace archetype with dimension scores. The model SHALL only write the interpretation for the computed archetype and SHALL NOT change it. Results SHALL NOT be stored server-side.

#### Scenario: Same answers
- **WHEN** a user submits the same answers twice
- **THEN** the archetype and dimension scores are identical.

#### Scenario: Incomplete answers
- **WHEN** a submission omits a question or uses an unknown option
- **THEN** the backend returns 422 VALIDATION_FAILED with the existing error contract.

### Requirement: Tarot reading
`POST /api/v1/ai/tarot` SHALL draw three distinct major-arcana cards server-side with a secure random source, assign past/present/future positions and upright or reversed orientation, and return an AI reading for an optional user question of at most 100 characters. The model SHALL interpret only the drawn cards.

#### Scenario: Draw
- **WHEN** a user asks a question and draws
- **THEN** the response contains three different cards from the 22-card deck with positions, orientations and readings.

### Requirement: Personal career map from a minimized digest
`POST /api/v1/ai/career-map` SHALL build a digest only from the current user's own journals of the last 30 business days and the user's own task assignments: tag frequencies, task status and priority counts, at most 20 own task-title excerpts of at most 30 characters each, and at most 40 entry excerpts of at most 60 characters each. Before sending, the digest SHALL replace enterprise member names with neutral placeholders and remove phone numbers, email addresses, URLs and digit sequences of six or more digits. No raw journal or task text beyond the excerpts SHALL leave the server. The result SHALL contain a career label, ability directions with evidence level, three development routes and suggested actions.

#### Scenario: Colleague names removed
- **WHEN** a journal entry mentions another enterprise member by display name
- **THEN** the digest sent to DeepSeek contains a placeholder instead of that name.

#### Scenario: No recent data
- **WHEN** the user has no journals or tasks in the window
- **THEN** the backend does not call DeepSeek and returns guidance content explaining that more records are needed.

### Requirement: Company outlook within existing permissions
`POST /api/v1/ai/company-outlook` SHALL build a digest only from data the current user can already read through existing endpoints: enterprise important item titles (at most 20, each at most 40 characters, never item content) and aggregate statistics of tasks of the last 90 days within the user's existing task visibility (counts by assignment status, priority and tag, overdue assignment count). Task titles SHALL NOT be sent. The digest SHALL apply the same redaction rules and SHALL NOT include individual names. The result SHALL contain exactly three scenarios (rapid growth, steady progress, challenge mode), each with possible developments, supporting signals from the digest and suggested preparations.

#### Scenario: Employee scope
- **WHEN** an employee without management scope requests the outlook
- **THEN** only enterprise important items and statistics of tasks that employee can already read are used.

#### Scenario: Signals reference the digest
- **WHEN** a scenario lists supporting signals
- **THEN** the signals are derived from the supplied digest and not from information outside it.
