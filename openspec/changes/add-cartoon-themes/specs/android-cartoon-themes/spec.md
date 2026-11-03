## ADDED Requirements

### Requirement: Nine selectable themes
The Android client SHALL offer the five existing themes (warm, mint, sky, sakura, lilac) followed by four cartoon themes (toon_cream 奶油单车, toon_breeze 凉风习习, toon_farm 农场乐园, toon_spring 春日郊游), all selectable in settings and saved per account as before.

#### Scenario: Select a cartoon theme
- **WHEN** a user selects 凉风习习 in the theme dialog
- **THEN** the app switches to that theme immediately and keeps it after restart for that account.

#### Scenario: Existing themes unchanged
- **WHEN** a user keeps or selects one of the five original themes
- **THEN** colors, vector navigation icons and card borders render exactly as before this change.

### Requirement: Cartoon theme tokens
Each cartoon theme SHALL derive its semantic color tokens from its reference artwork and SHALL meet the same contrast rules as the original themes: white text on primary and pressed primary at least 4.5:1, readable text and primary links at least 4.5:1 on every background layer, and textPrimary at least 4.5:1 on primaryContainer.

#### Scenario: Contrast check
- **WHEN** the design token unit tests run
- **THEN** all nine themes pass every contrast assertion.

### Requirement: Hand-drawn navigation and action art
In a cartoon theme the bottom navigation SHALL show that theme's five hand-drawn icons in navigation order with the unchanged labels 导图、视图、日志、AI地图、我的; the selected item SHALL be indicated by the primary-colored label and a soft indicator, and unselected icons SHALL remain fully visible. The calendar add-task button SHALL use the theme's hand-drawn "+" artwork with the content description 添加任务.

#### Scenario: Navigation icons
- **WHEN** the user switches to 农场乐园
- **THEN** the five navigation icons change to the farm artwork while labels, order and badges stay the same.

### Requirement: Scene illustration
In a cartoon theme the primary pages SHALL show the theme scene illustration anchored above the bottom navigation behind page content; the navigation bar SHALL use the scene ground color so the scene appears continuous. The calendar view SHALL show the illustration faded so that it never reduces the legibility of calendar items. Secondary pages, dialogs and sheets SHALL NOT show the scene.

#### Scenario: Calendar legibility
- **WHEN** the week view is open in a cartoon theme
- **THEN** tasks and time labels draw above the faded illustration and remain readable.

### Requirement: Cartoon card outline
In a cartoon theme shared panels, cards and item containers SHALL use a 2dp dark outline instead of the 1dp neutral border, keeping corner radii and spacing from the design system.

#### Scenario: Card outline
- **WHEN** the home dashboard renders in 奶油单车
- **THEN** its panels show the dark 2dp outline, and the same panels in 暖米 keep the 1dp neutral border.

### Requirement: Asset attribution
The repository SHALL document that the cartoon artwork was cropped from 番茄ToDo screenshots for course demonstration only and is not for release or commercial use.

#### Scenario: Attribution present
- **WHEN** a reviewer opens the Android README or this change
- **THEN** the source and usage limitation of the artwork are stated.
