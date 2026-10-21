## 1. Review and implementation
- [x] 1.1 Guard password renewal against concurrent old-token 401s, release the guard on every outcome, and propagate cancellation.
- [x] 1.2 Scope the pending review effect to the authenticated account and permission; include task kind in dirty detection and retain optional tags.
## 2. Verification
- [x] 2.1 Add local HTTP regression coverage for renewal concurrency, failed renewal and normal 401 behavior.
- [x] 2.2 Run Android unit tests and APK build, strict OpenSpec validation, and record integrated module checks before merging PR #23.
