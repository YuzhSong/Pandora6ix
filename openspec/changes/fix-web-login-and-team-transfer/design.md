# Design

## Context
App uses hash routing with personnel as fallback; session notifications drive login state. Personnel lifecycle calls an atomic backend command, whose database errors are currently presented in English.

## Goals / Non-Goals
Preserve authenticated deep links and local untracked work. Do not change production personnel data during diagnosis or add API contracts.

## Decisions
Reset the hash and active page on a new authenticated session, and use organizations for invalid hashes. Remove the redundant footer paragraph. Exercise cross-team transfers through the actual backend controller, checking history, profile preservation, and session revocation before deciding whether a backend fix is needed. Translate DATA_CONFLICT without inventing a specific database cause.

## Risks / Trade-offs
Production data or schema may differ from current migrations; a passing local test cannot identify the failing production constraint. Document that limitation if it remains unresolved.

## Directory and log readability refinement
Use one in-service membership selector for organization lists and counts, preserving disabled but non-departed people. Translate snapshot keys and values only at display time; retain raw comparison equality. Resolve IDs from the current authorized directory and retain unavailable IDs in expandable operation metadata. Use higher-specificity modal sizing so shared editor styles cannot narrow it. Authentication still requires enterpriseDomain; removing that API field requires backend account-namespace decisions and Android/Web coordination.

## Copy cleanup
Remove implementation-oriented explanation paragraphs from the UI per user direction, including the current-name resolution note. Preserve validation, operational failure/status feedback, confirmation consequences and raw metadata. Current-name resolution and its historical limitations remain documented here and in the backend handoff.

## Personnel history readability
Display personnel changes in recording-time order from earliest to latest, with numbered timeline events and ordinal job-adjustment headings. Show changed fields in explicit before/after columns, preserving original values and timestamps. Use the same modal width and maximum height as log details, with internal scrolling and mobile wrapping.
