## Context

PR #8 changes only task-entry recipient selection and one mock-data whitespace line; its branch already includes the last dev integration. PR #9 changes only Web tooling and documentation. Backend production code currently exposes HealthController and infrastructure error handling, with empty application interfaces.

## Goals / Non-Goals

**Goals:** Integrate the two reviewed PRs, retain existing task tags and guards, verify keyboard-safe recipient selection, verify the actual Spring health route through the Web proxy, and deliver a requirements-linked backend gap inventory.

**Non-Goals:** Implement missing business APIs, select authentication/database technologies, convert prototype state into formal DTOs, or resolve Q-01 through Q-09 without stakeholder confirmation.

## Decisions

- Use the existing current-dev content as the merge base and review the exact incoming heads. Earlier branch wording about missing calendar tag colors is historical; current dev already provides those colors.
- Review the recipient candidate layer on an emulator with the IME open. If the in-layout candidate column changes form measurement or clips candidates, replace it with a separately positioned, bounded scrollable popup anchored above the query field. A real overlay avoids shifting the form while typing.
- Keep the query visible as selected chips grow. Restore sheet visibility when the user chooses to continue editing after Android back has hidden the modal sheet, preserving its remembered form state.
- Keep health raw JSON explicitly opt-in and preserve the default business envelope. Start a temporary local Spring process on an unused loopback port and test the actual Vite rewrite and representative missing API routes.
- Derive backend coverage from production mappings, use-case implementations, persistence/security dependencies, requirement IDs and API0.1 routes. Keep Planned, Conditional/Pending, Reserved and client-only settings separate; draft routes are not final contracts.
- Write a dated audit document under docs/reviews with relative source links, reviewed hashes, validation results and recommended delivery order. Keep the five design master documents in their existing directories. Do not treat empty interfaces, enums, local repositories or a passing health check as implemented business functionality.

## Risks / Trade-offs

- API0.1 lacks company-initialization and statistics route definitions → identify the functional gap without inventing route names.
- The candidate directory is still mock data → explicitly retain prototype scope and identify authorized recipient search as a missing backend endpoint.
- Q-01 through Q-09 affect workflow and time semantics → retain them as decisions required before relevant implementation.
- Emulator and local-loopback probes do not prove deployment or real-device readiness → describe exactly the environment tested.
