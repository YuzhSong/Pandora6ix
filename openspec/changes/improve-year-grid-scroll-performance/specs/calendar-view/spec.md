## ADDED Requirements

### Requirement: Lightweight year date rendering
The Android year grid SHALL avoid a separate text layout node for every date digit while retaining lazy month creation, existing font-scale-aware typography, today's highlight, task markers, and independent date click targets. Every date SHALL retain a full year/month/day accessibility description. Month title navigation, live year title updates and edge recentering SHALL retain their existing behavior. Performance claims SHALL identify the build configuration and measurement conditions.

#### Scenario: Scroll to another month
- **WHEN** a new month card enters the year viewport
- **THEN** date labels use cached month-level rendering and the visible dates retain independent click actions

#### Scenario: Read or select a date
- **WHEN** a user explores a date with accessibility tools or taps it
- **THEN** its full date is available and selection dispatches that date, including adjacent-month cells

#### Scenario: Theme or font scale changes
- **WHEN** theme colors or font scale change
- **THEN** cached date rendering updates without stale colors, broken task markers or displaced hit targets

#### Scenario: Fling across a year boundary
- **WHEN** the user flings across year boundaries
- **THEN** the title follows the viewport immediately while task-year queries and edge recentering wait until scrolling settles, preserving the ongoing gesture
