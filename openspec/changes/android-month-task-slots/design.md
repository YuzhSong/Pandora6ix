## Context

Month rows are fixed at 150dp, but the current date row plus three 23dp bars and a count row leaves excess space and makes labels look low-density.

## Decisions

- Keep the weekly row fixed at 180dp, reserve 34dp for dates, and split the remaining task area into four equal weighted slots.
- If visible tasks are four or fewer, render each task in its own weighted slot and leave remaining slots empty.
- If visible tasks exceed four, render the first three tasks and put `+${count - 3}条计划` centered in the fourth slot.
- Let task bars fill their slot height, center text vertically, and use 13sp labels.

## Non-Goals

No changes to task priority semantics, task ordering, or Week view.
