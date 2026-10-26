## Context

Journal edits use publication day in the enterprise business timezone. The user confirmed on 2026-10-27 that logs backfilled today for either of the preceding two days remain editable and deletable today.

## Goals / Non-Goals

Goals: implement the confirmed proposal while preserving existing navigation and service boundaries. Non-goals: new business APIs beyond the proposal, or unrelated UI redesign.

## Decisions

Compute permissions from published_at and share the same predicate between allowedActions and mutations. Keep the three-date creation window. Fetch complete owner date metadata separately from paginated journal bodies; Android uses server businessToday and allowedActions, avoiding device-clock authorization.

## Risks / Trade-offs

Keyboard, calendar and navigation regressions require device acceptance; builds and automated tests do not establish physical-device behavior.

## Migration Plan

Deploy the compatible backend before the Android client when required. No destructive migration is needed.
