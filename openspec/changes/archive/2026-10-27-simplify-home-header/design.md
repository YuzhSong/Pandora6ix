## Context

Settings already has an entry in the personal page.

## Goals / Non-Goals

Goals: implement the confirmed proposal while preserving existing navigation and service boundaries. Non-goals: new business APIs beyond the proposal, or unrelated UI redesign.

## Decisions

Remove only the redundant HomeScreen settings callback and button. Keep notification entry and existing personal settings routes.

## Risks / Trade-offs

Keyboard, calendar and navigation regressions require device acceptance; builds and automated tests do not establish physical-device behavior.

## Migration Plan

Deploy the compatible backend before the Android client when required. No destructive migration is needed.
