## 1. Implementation

- [x] 1.1 Expose permission-aware scope controls in all calendar modes with single-line scrolling labels.
- [x] 1.2 Pass selected scope to day/week/month loading using server calendar ranges and existing task queries.

## 2. Validation

- [x] 2.1 Review scope propagation, range boundaries, pagination, and permission fallback; run strict OpenSpec and whitespace checks.
- [ ] 2.2 Compile Android and run unit tests when Gradle cache access is available.

## Verification notes

- Strict OpenSpec validation and `git diff --check` passed.
- Source review: all day/week/month requests receive taskScope; non-mine queries reuse server-provided day, Monday-to-Sunday week, and padded month boundaries and follow task pagination. Existing year query, saved scope, tag filtering, review-capability gate, and managed 403 fallback are preserved.
- Compilation/unit tests and device verification remain pending. The earlier Gradle attempt in this session could not access the user-cache lock, and the elevated retry was declined; that permission request has not been repeated.
