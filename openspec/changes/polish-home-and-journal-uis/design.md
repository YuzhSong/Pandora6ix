## Context

Home panels and journal lists need usable empty/loading/error states and direct detail navigation.

## Goals / Non-Goals

Goals: implement the confirmed proposal while preserving existing navigation and service boundaries. Non-goals: new business APIs beyond the proposal, or unrelated UI redesign.

## Decisions

Keep journal detail routes keyed by journal ID and task detail routes keyed by task ID. Close expanded panels before opening details. Reuse persisted journal data and real tag colors; keep editing actions inside the corresponding journal card.

## Risks / Trade-offs

Keyboard, calendar and navigation regressions require device acceptance; builds and automated tests do not establish physical-device behavior.

## Migration Plan

Deploy the compatible backend before the Android client when required. No destructive migration is needed.
