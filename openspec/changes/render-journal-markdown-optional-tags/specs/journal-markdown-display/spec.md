## ADDED Requirements

### Requirement: Render submitted journal Markdown and preserve source

Android SHALL accept plain text and Markdown source in the journal editor and store the source through existing APIs. Journal cards and journal detail SHALL render headings, paragraphs, emphasis, lists, quotes, code, links, strikethrough and read-only task lists. Plain text line breaks SHALL remain visible. Cards SHALL keep a three-line preview and open detail on tap; detail SHALL display the full body. Editing an existing journal SHALL restore its source rather than rendered text. No remote images or HTML rendering are required.

#### Scenario: Submit prefilled daily items
- **WHEN** a user submits a journal containing prefilled `- item` source
- **THEN** the journal card and detail show a rendered bullet list instead of literal Markdown markers

#### Scenario: Mixed Markdown
- **WHEN** a journal contains a heading, bold text, a quote, code, a link, strikethrough and checked/unchecked task items
- **THEN** detail renders the respective formatting and read-only task states

#### Scenario: Plain text and editing
- **WHEN** a user submits ordinary multiline text and later reopens the editor
- **THEN** reading preserves its line breaks and editing restores the original source

### Requirement: Plain text after a list starts at the left edge

An unindented non-list line immediately following a journal list SHALL end the list and render as a top-level paragraph even without a blank separator. Explicitly indented list continuations, nested lists and fenced code contents SHALL retain their structure. This adaptation SHALL affect rendering only, preserving the saved source. The journal editor SHALL NOT show the explanatory “支持普通文本和 Markdown，发布后显示排版效果” text.

#### Scenario: Plain text directly below a todo
- **WHEN** source contains `- [ ] 待办` followed by an unindented `补充说明` on the next line
- **THEN** 补充说明 renders outside the list, aligned with the main content's left edge

#### Scenario: Indented continuation and code
- **WHEN** text is explicitly indented under a list item or appears inside a fenced code block
- **THEN** rendering preserves that indentation and code content

### Requirement: Compact journal presentation

Journal editor section labels SHALL be “日志日期”, “标签” and “完成的工作”, without leading decorative dots or a parenthesized tag explanation. Markdown block boundaries SHALL use a single line break instead of adding empty lines for each enclosing block. Line breaks inside paragraphs and fenced code SHALL be retained.

#### Scenario: List followed by a note
- **WHEN** a list or nested list is followed by an ordinary paragraph
- **THEN** the paragraph begins on the next line at the content left edge without extra blank lines inserted by the renderer
