## Context

Personal important-item editing and journal entry layouts contain redundant helper text and keyboard-dependent headings.

## Goals / Non-Goals

Goals: implement the confirmed proposal while preserving existing navigation and service boundaries. Non-goals: new business APIs beyond the proposal, or unrelated UI redesign.

## Decisions

Keep headings outside the keyboard-dependent branch, remove redundant helper text, and retain a scrollable 90-percent-height editor. Preserve existing save and selection actions without adding APIs.

## Risks / Trade-offs

Keyboard, calendar and navigation regressions require device acceptance; builds and automated tests do not establish physical-device behavior.

## Migration Plan

Deploy the compatible backend before the Android client when required. No destructive migration is needed.
