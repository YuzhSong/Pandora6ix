# Tasks

## 1. Contract and data

- [x] 1.1 Define list/detail/snapshot contracts and backend checklist; verify API adapter tests cover filters, invalid responses and missing endpoints.
- [x] 1.2 Implement safe labelled-field comparison and complete Excel export; verify tests cover deleted/created fields, sensitive data, pagination and literal Excel values.

## 2. Web interface

- [x] 2.1 Implement themed filter/list/pagination/detail/version views; verify loading/error/empty, keyboard dialog and version switching with controlled browser responses.
- [x] 2.2 Integrate current workspace refresh and session cancellation; verify stale requests cannot refill obsolete results and exports use all matching pages.

## 3. Integration verification

- [x] 3.1 Run Web tests, build, lint and strict OpenSpec validation; record results and backend integration limitations.
- [x] 3.2 Inspect desktop and narrow-screen local browser flows; verify no backend files or unrelated drafts were modified.

## 4. Customer visual refinement

- [x] 4.1 Simplify filters, compact summary rows and repair pagination spacing; verify desktop and narrow-screen rendering.
- [x] 4.2 Show two full versions with text differences by default, collapse metadata and separate history; verify text-diff tests and browser tab/version flows.
- [x] 4.3 Run affected checks and strict OpenSpec validation, record final local preview evidence.

## 5. Integrated preview and controls

- [x] 5.1 Fix pages at 20, unify pagination typography and implement themed calendar range selection; verify keyboard/date filtering and browser styling.
- [x] 5.2 Add explicitly flagged local-only sample logs to the full application; test filtering, snapshots and multi-page export and verify fixtures are absent from production output.
- [x] 5.3 Run affected checks, inspect the full local app and document the isolated preview boundary.

## 6. Direct filters and module colors

- [x] 6.1 Show every filter directly in a compact row and apply distinct accessible light/dark module badge colors; verify the complete local page, narrow layouts and Web build.

## 7. Date entry and clear operation choices

- [x] 7.1 Support typed dates and month/year navigation with invalid-date validation, rename date reset, and verify calendar/typed selection together.
- [x] 7.2 Use contextual operation choices, distinguish module colors from each other and result colors, and verify module switching and the full local page.

## Workflow follow-up

- Agree and implement backend contract before real recording can be tested.
- Submit for non-author review when requested; deploy only through approved dev workflow.
