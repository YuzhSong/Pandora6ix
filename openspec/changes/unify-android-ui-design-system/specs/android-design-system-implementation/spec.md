## ADDED Requirements

### Requirement: Single theme adapter
The Android client SHALL derive the Pandora semantic color tokens, the `LocalAppTheme` values and the Material3 color scheme from the same selected theme key, so that one theme never renders two different primary action colors.

#### Scenario: User switches theme
- **WHEN** the user selects warm, mint, sky, sakura or lilac in settings
- **THEN** page backgrounds, surfaces, primary actions, selected states and Material components all change together and the selection persists per account as before.

#### Scenario: Primary action contrast
- **WHEN** any of the five themes renders white text on its primary action color
- **THEN** the contrast ratio SHALL be at least 4.5:1.

### Requirement: Shared typography, spacing and shape tokens
The Android client SHALL provide the eight typography levels (28/24/20/18/16/14/14/12sp) plus the calendar-only micro style, the 4dp-based spacing scale and the radius tokens (8/12/16/20/24dp, pill, circle) as code tokens mapped into Material3 Typography and Shapes.

#### Scenario: New screen code
- **WHEN** a developer styles text, padding or corners in a migrated screen
- **THEN** they use the named tokens instead of local sp/dp literals, except for documented calendar geometry exceptions.

### Requirement: Shared component variants
The Android client SHALL render buttons, panels, dialogs, bottom sheets, text fields, tag chips, status chips, tabs and list rows through shared Pandora components with defined default, pressed, disabled, loading and error states.

#### Scenario: Confirmation dialog
- **WHEN** any confirmation or alert dialog opens in any theme
- **THEN** it uses the white elevated surface, 24dp corners, the 40% black scrim, a TitleMedium title, cancel before confirm, and a destructive confirm uses the Error color.

#### Scenario: Status presentation
- **WHEN** a task, assignment or review status is shown
- **THEN** its color comes from the central status mapping and is always accompanied by text.

### Requirement: Business behavior preserved
The UI unification SHALL NOT change navigation, repositories, API calls, permissions, task and journal workflows, or the information structure of the calendar, task summary and organization views.

#### Scenario: Regression review
- **WHEN** the change diff is reviewed
- **THEN** the bottom navigation still has exactly 导图、视图、日志、AI地图、我的 in that order, no backend or data-layer semantics changed, and the existing unit tests pass.
