## Context
Web has grouped staff cards, a full directory snapshot, atomic successor replacement, versioned user PATCH and authenticated binary avatar GET/JSON PUT. No explicit disable-reason field exists.
## Goals / Non-Goals
Improve comparison, status clarity, successor sequencing and durable avatars. Do not invent historical reasons, bypass manager invariants, reactivate departed users, or upload private production data during tests.
## Decisions
- Keep card/group mode and provide a paged table over the same filtered result; reset employment, organization, role, keyword and collapsed/page state together.
- Use cycle-safe descendant traversal for inclusive counts; count only non-departed personnel, including disabled in-service accounts.
- Expose confirmed reactivation only for in-service disabled accounts. Preserve profile/roles/version, validate response identity/status, and require reload after uncertain failure. No password reset as a side effect. Show unknown disable reasons explicitly.
- Before manager departure or job adjustment, guide through existing atomic replacement. Continue only after the first request succeeds, using the returned person/version. Never automatically submit the final departure or transfer.
- Decode and downscale avatar images in-browser, cap JSON data URLs below the backend 768000-character limit, PUT with the API client and read binary through authenticated fetch. Cancel stale image requests and revoke object URLs; use name fallback on 404. Persist through backend, not localStorage.
## Risks / Trade-offs
Historical disable reasons cannot be recovered from the current contract. Reactivation remains subject to backend authorization and version checks. Avatar and restoration can fail on an older deployment and must display actual failures. Large tables use pagination, cards remain available.
