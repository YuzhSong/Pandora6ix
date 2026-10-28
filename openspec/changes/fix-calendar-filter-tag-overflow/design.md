## Context

CalendarScreen places all selected tag texts in a constrained Row alongside the prefix and clear button. Overflow can compress labels and increase row height.

## Goals / Non-Goals

**Goals:** Keep selected tags in one line, make overflow reachable, and retain usable calendar content and clear action.

**Non-Goals:** Change filtering semantics, dialog selection, APIs, or other screens.

## Decisions

Use a weighted inner Row with horizontalScroll and rememberScrollState between the fixed prefix and clear button. Each label uses maxLines = 1 and softWrap = false. This keeps the scroll viewport bounded while measuring its tags without horizontal compression. Keep the row outside calendar navigation gesture handlers, so scrolling tags cannot change the calendar date. A wrapping FlowRow would consume vertical space; scrolling the whole summary would hide the clear action.

## Risks / Trade-offs

Very long names require horizontal scrolling → preserve full text in the scrollable strip. Large selections compose all selected tags → consistent with the existing tag summary and modest enterprise tag catalog.
