## ADDED Requirements

### Requirement: AI explorer home in the existing tab
The Android client SHALL replace the "功能规划中" placeholder of the fourth primary tab with the AI explorer home, keeping the bottom navigation labels, icons and order (导图、视图、日志、AI地图、我的) unchanged. The home SHALL show a daily fortune entry, a "工作实验室" group (我的职业星图、公司下一站) and a "灵感游乐场" group (职场人格盲盒、AI 塔罗屋).

#### Scenario: Open AI tab
- **WHEN** a signed-in user taps AI地图
- **THEN** the AI explorer home appears with the five feature entries and the bottom navigation is unchanged.

### Requirement: Design system consistency
AI screens SHALL use the Pandora design system tokens and shared components for typography, buttons, dialogs, spacing and system bars in all five themes. Feature cards MAY use feature accent colors or gradients for decoration only, while text on them SHALL meet 4.5:1 contrast.

#### Scenario: Theme switch
- **WHEN** the user switches among the five themes
- **THEN** AI screens follow the theme's background, primary actions and text colors, and feature accents remain legible.

### Requirement: Loading, fallback and error states
Each feature SHALL show a progress state while generating, render structured results as cards rather than raw text, show a "基础版内容" hint when `aiGenerated=false`, show a quota hint when `quotaExceeded=true`, and offer retry on network failure without losing the user's inputs. Leaving a feature with system back SHALL return to the AI explorer home.

#### Scenario: Network failure
- **WHEN** a tarot request fails because the network is unavailable
- **THEN** the client shows a retry action and keeps the typed question.

#### Scenario: Back navigation
- **WHEN** the user presses system back on a feature page
- **THEN** the AI explorer home is shown instead of exiting the app.

### Requirement: Disclaimer and data notice
Every result page SHALL display the disclaimer returned by the backend. 我的职业星图 and 公司下一站 SHALL show, before the first generation, a notice that a de-identified summary and keywords of the user's visible work records will be sent to the third-party DeepSeek service. The tarot question field SHALL remind users not to enter sensitive information.

#### Scenario: First career map
- **WHEN** a user opens 我的职业星图 and has not generated one before on this device
- **THEN** the data notice is shown and generation starts only after the user confirms.

#### Scenario: Notice already confirmed
- **WHEN** a user who confirmed the notice on this device opens 我的职业星图 or 公司下一站
- **THEN** generation starts immediately without another tap.

### Requirement: Feature interactions
The client SHALL provide: fortune with an optional zodiac choice; a one-question-per-screen personality test with progress and a result card; a three-card tarot flow with a flip animation per card before the reading; a career map with label, ability directions, three selectable routes and actions; and a company outlook with three scenario cards that expand to developments, signals and preparations. The client SHALL keep the latest personality result and the "data notice confirmed" flag per account on the device only.

#### Scenario: Personality result reopened
- **WHEN** a user who completed the personality test reopens it later
- **THEN** the last result card is shown with an option to test again.
