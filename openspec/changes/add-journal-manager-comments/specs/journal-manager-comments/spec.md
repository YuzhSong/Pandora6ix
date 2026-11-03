## ADDED Requirements

### Requirement: Journal author profile
`GET /api/v1/work-logs/{id}` SHALL include an `author` object with the author's id, display name, job title, organization name and department name (the nearest DEPARTMENT ancestor of the author's organization unit, or null). Visibility of the journal itself SHALL remain unchanged.

#### Scenario: Manager opens a subordinate journal
- **WHEN** a manager opens a journal written by a member of their team
- **THEN** the response contains the member's display name, job title and department name.

### Requirement: Manager comments on journals
The backend SHALL let an actor post a comment on a journal only when the actor can already read that journal and is not its author (the author is within the actor's management scope). Comment content SHALL be 1–500 characters after trimming. The journal author and every actor who can read the journal SHALL be able to list its comments in creation order; the list response SHALL state whether the current actor may comment. Only the comment's writer SHALL be able to delete it. Deleting a journal SHALL delete its comments.

#### Scenario: Manager comments
- **WHEN** a team lead posts a comment on a subordinate's journal
- **THEN** the comment is stored with the lead as writer and appears in the journal's comment list for the lead and the subordinate.

#### Scenario: Author cannot comment on own journal
- **WHEN** the journal author tries to post a comment
- **THEN** the backend returns 403 and stores nothing.

#### Scenario: Unrelated colleague
- **WHEN** a peer outside the author's management chain lists or posts comments
- **THEN** the backend returns the uniform 404 used for unreadable journals.

#### Scenario: Delete own comment only
- **WHEN** someone other than the writer deletes a comment on a journal they can read
- **THEN** the backend returns 403 and the comment remains.

### Requirement: Android journal detail comments
The Android journal detail SHALL show the author's name, department and job title at the top, and below the journal content a "上级留言" section listing comments with writer name, job title, time and content. Actors allowed to comment SHALL see a multi-line input of at least four lines with a character counter and a send button; the author SHALL see the list read-only with an empty-state hint when there are no comments. Writers SHALL be able to delete their own comments after confirmation.

#### Scenario: Send a comment
- **WHEN** a manager types a comment and taps 发送
- **THEN** the button shows a sending state, the comment appears in the list on success and the input clears; on failure the typed text is kept with an error message.
