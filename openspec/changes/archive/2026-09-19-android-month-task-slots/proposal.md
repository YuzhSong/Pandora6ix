## Why

Month task bars currently occupy only a short fixed height, leaving unused space in each fixed weekly row. The review needs the task area to use four equal slots so labels are easier to read and overflow is predictable.

## What Changes

- Divide each fixed Month week task area into four equal-height slots.
- Show up to four tasks, one per slot; when there are more, show the first three tasks and `+N条计划` in the fourth slot.
- Vertically center and enlarge task labels inside taller bars.

## Capabilities

### New Capabilities
- `month-task-slots`: Equal-height Month task slots and overflow placement.

### Modified Capabilities

## Impact

- Android Compose Month view only; task data and navigation remain unchanged.
