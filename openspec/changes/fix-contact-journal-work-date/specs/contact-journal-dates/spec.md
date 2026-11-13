## ADDED Requirements

### Requirement: Distinguish journal work date from publication time

Android contacts journal rows SHALL always show workDate with the label “日志日期：” in subordinate lists, per-member lists and recent journal previews. Work dates SHALL NOT be converted by timezone. Missing work dates SHALL show “未知”. Publication time, when its formatted value is nonblank, SHALL appear separately with the label “发布时间：” using the existing business timezone formatter.

#### Scenario: Backfilled journal
- **WHEN** a journal has workDate 2026-11-11 and was published on 2026-11-13
- **THEN** its row shows “日志日期：2026-11-11” and a separately labelled publication time

#### Scenario: No publication time
- **WHEN** a journal has workDate 2026-11-11 and no publication time
- **THEN** its row still shows “日志日期：2026-11-11” and omits the publication time line

#### Scenario: Missing work date
- **WHEN** a journal has a blank workDate and a publication time
- **THEN** its row shows “日志日期：未知” and the separately labelled publication time
