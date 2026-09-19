## Context

The current Month view uses a vertical scroll and fixed 180dp rows. That makes the fifth week inaccessible without swiping. The target prototype prioritizes direct visibility on the phone.

## Decisions

- Use exactly 35 cells (five weeks) for the prototype month grid and remove the Month `verticalScroll` modifier.
- Let the calendar body take the remaining screen height and give each of five week rows equal weight.
- Within each row, reserve three equal task slots: first two for task bars, third for `+${count - 2}条计划` when more than two tasks are visible.

## Trade-offs

- Months whose calendar needs six rows are outside this low-fidelity layout assumption; the prototype remains optimized for the supplied September scenario.
