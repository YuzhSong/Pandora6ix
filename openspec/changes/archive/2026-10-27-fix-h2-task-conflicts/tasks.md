## 1. Reproduction and repair

- [x] 1.1 Confirm cloud error class and reproduce valid CHECK writes failing after the creator connection closes on H2 2.4.240.
- [x] 1.2 Pin released fixed H2 engine and add creator-retirement plus file-reopen regression tests.
- [x] 1.3 Improve sanitized constraint identifier extraction and add log-metadata unit coverage.

## 2. Verification

- [x] 2.1 Cover personal/delegated, all-day/timed/cross-day creation after pool eviction and normal approval flow.
- [x] 2.2 Verify a disposable file made by H2 2.4.240 can be opened and written by the fixed engine.
- [x] 2.3 Run backend verification, OpenAPI, packaged runtime and strict OpenSpec validation; record exact review and deployment limitations.
