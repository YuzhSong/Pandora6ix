## Context

TaskSummaryFlow owns shared authenticated task/candidate/journal pagers above destination composition. Its overview currently uses multiple lazy surfaces for tasks and exposes inline candidate pagination. The existing surface color is almost identical to the warm page background.

## Goals / Non-Goals

**Goals:** Three clearly bounded white panels; three-item previews; task filters affect only the first panel; a task-only paginated list; preserved filters, parent scroll and detail behavior.

**Non-Goals:** Database schema changes, exposing personnel secrets through candidate lists, KPI/metrics, calendar or bottom-navigation changes, modifying unrelated team behavior.

## Decisions

- Make each overview panel a single existing SummaryPanel surface, using MaterialTheme's existing white surface, 16dp radius, a subtle HairLine border and 16dp separation. Retain typography, accent, tags, avatar and detail components.
- Limit task/journal previews to three and direct-report previews to four. Apply limits after scope/direct-report filtering. Keep all fetched records in the existing pager: previews must not truncate the source collection.
- Place task tabs at the top of the first panel, with “查看全部任务” below its preview. Changing a tab only changes taskScope, not page. Add one `tasks` destination, retaining that scope and reusing the same pager; no other summary panels or scope subpages appear there.
- Direct-report and journal panels expose “查看全部” to existing organization/journal destinations. No preview panel exposes next-page controls. Errors still retry; partial empty candidate results point to the full organization page.
- Keep destination SaveableStateHolder and pagers above navigation. Returning from tasks/details preserves the chosen filter and overview scroll. Full-list refresh only refreshes its own collection.
- Extend these boundaries to every secondary destination: member detail has separate personal-information, responsible-task and recent-journal panels; task/journal rows have individual soft theme backgrounds, subtle borders and spacing. Full task/journal/organization destinations put their scrollable content inside a white panel. Member “查看全部” retains its author/executor filter, and back returns to that member without changing the overview task scope.
- Compact task rows with explicit text line heights, 8dp vertical padding, 5dp row gaps, smaller chips and 22dp executor avatars. Preserve status, tags, priority, due date and detail click target. Use a lighter 0.75dp entry border and retain gaps between entries.
- Remove the normal-state managed-task caption and organization implementation note. Retain actionable permission, empty, loading and error states.
- Add a dedicated MemberContactService/Controller using ScopeService.activeManaged and enterprise-scoped reads. Manager authorization is checked before exposing id and phoneNumber; unavailable/out-of-scope members return uniform 404, non-managers return 403. Candidate responses continue excluding personnel secrets.
- Load contact only while member detail is open through UserRepository, with composition-scoped state and cancellation propagation. Refresh/retry refetches it. Do not persist contact data or fall back to administrative endpoints. A missing phone is “未填写”; unsupported deployed backend is “暂不可用”; other failures are retryable.

## Risks / Trade-offs

- Some direct reports may be beyond the first authorized candidate page → explain partial empty results and use full organization pagination; do not falsely claim there are no reports.
- Small screens and large fonts → allow tab text to wrap, rows to grow, and panel body to scroll as part of the page; cap only item count, not panel height.
- No new unit tests for a fixed `take(3)` presentation rule. Use existing meaningful data tests plus build, source review and authenticated emulator interaction/screenshot checks where the current session permits.
