## ADDED Requirements

### Requirement: Executor names from assignments
The task summary list SHALL label executor avatars with the assignment's `recipientName` when present, falling back to the current user's name or the managed-member list, and only then to the "成员 xxxx" placeholder.

#### Scenario: Peer executor
- **WHEN** a delegated task includes a peer who is not in the viewer's managed members
- **THEN** the avatar shows the first character of the peer's real name instead of "成".
