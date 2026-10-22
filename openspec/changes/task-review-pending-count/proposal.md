# Proposal

## Why

The Android client currently downloads up to 100 pending review records just to render an approval badge. A dedicated count lets the client refresh that badge frequently with a small response while preserving the backend's authorization and current-review filtering.

## What Changes

- Add an authenticated endpoint returning the current user's pending task-review count.
- Keep the count consistent with the existing pending queue: only the designated reviewer and latest current pending attempts count.
- Cover empty, populated, unauthorized, and post-decision counts in backend integration tests.

## Capabilities

### New Capabilities

- None.

### Modified Capabilities

- `backend-task-review`: expose an authorized count for the same current pending review queue.

## Impact

Adds `GET /api/v1/task-reviews/pending-count` to the backend. No database migration or external dependency is required; Android can consume it instead of downloading review details to draw a badge.
