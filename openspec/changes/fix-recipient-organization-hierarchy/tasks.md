## 1. Model the organization hierarchy

- [x] 1.1 Build a pure Kotlin recipient tree using stable department/team IDs, direct members and legacy/unassigned fallbacks.
- [x] 1.2 Add focused tests for multiple levels, duplicate labels, missing metadata and duplicate people.

## 2. Integrate Android selection

- [x] 2.1 Render expandable department/team nodes with descendant counts, preserving person-ID selection and search behavior.
- [x] 2.2 Show loading/failure/retry states without stale search results or lost selections.

## 3. Verify and update the pending PR

- [x] 3.1 Run Android unit tests and assembleDebug, strict OpenSpec validation and diff checks; confirm no journal behavior changes.
- [x] 3.2 Update PR #16 source and description with validation evidence; leave both PRs unmerged.
