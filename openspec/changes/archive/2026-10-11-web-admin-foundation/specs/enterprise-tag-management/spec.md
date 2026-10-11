# Spec Delta

## Purpose

提供 Web 管理员对当前企业共用标签名称及颜色的演示维护能力，保持任务与日报共享一个平面标签目录，并为正式接口接入保留字段和版本约束，避免提前确定尚未确认的历史删除策略。

## ADDED Requirements

### Requirement: Flat enterprise tag catalog
The client SHALL list, search, add and edit enterprise tags with a nonempty name and a valid hexadecimal color, without building a parent-child tag hierarchy or treating tag creation as task approval authorization.

#### Scenario: Edit a tag color
- **WHEN** an administrator changes a tag's name or color and saves valid input
- **THEN** the catalog reflects that change after reload

#### Scenario: Invalid color
- **WHEN** a tag color is not in #RRGGBB format
- **THEN** saving is rejected with a validation message

### Requirement: Respect pending lifecycle policy
The client SHALL omit tag deletion and disabling operations until the historical-reference policy is confirmed.

#### Scenario: Browse tag actions
- **WHEN** an administrator browses the tag catalog
- **THEN** name and color maintenance is available and deletion or disabling is unavailable

### Requirement: Detect stale resource edits
Mutable demo resources SHALL reject an outdated expected version rather than overwrite newer edits.

#### Scenario: Two tabs edit a tag
- **WHEN** another tab has saved the tag after a form was opened
- **THEN** the stale form save reports conflict and keeps its unsaved content
