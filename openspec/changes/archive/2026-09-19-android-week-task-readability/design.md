## Decisions

- Increase the Week task bar to 44dp and use 13sp text, matching the Month task bar's readable scale.
- Keep one line to preserve the continuous gantt layout, but use `TextOverflow.Clip` instead of an ellipsis so no `...` is inserted.
- Preserve the existing date-span width calculation and click behavior.
