## 1. Implementation

- [x] 1.1 Constrain selected tags to a weighted horizontal scrolling row with single-line labels and fixed prefix/clear controls.

## 2. Validation

- [ ] 2.1 Run Android compile and existing unit tests; review layout constraints and gesture boundaries for overflow and clearing.
- [x] 2.2 Run strict OpenSpec validation and record verification limits.

## Verification notes

- `openspec validate fix-calendar-filter-tag-overflow --strict` passed.
- `git diff --check` passed.
- Source review confirms the weighted tag viewport reserves space for the prefix and clear button, labels cannot wrap, and the strip is outside calendar gesture handlers. Filtering and clear state updates remain unchanged.
- Android compile/unit tests were attempted with `gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --offline`, but access to the Gradle user-cache lock was denied by the sandbox. The elevated retry was declined. Task 2.1 remains pending; runtime layout and gesture verification have not been performed.
