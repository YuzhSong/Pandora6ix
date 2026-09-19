## ADDED Requirements

### Requirement: Bottom navigation uses semantic outline icons
The five bottom navigation destinations SHALL use distinct icons matching their destination meaning and the supplied reference direction.

#### Scenario: Open the app
- **WHEN** the main screen is displayed
- **THEN** 首页 uses a four-square grid icon, 视图 uses a calendar icon, 日志 uses a note-and-pencil icon, AI地图 uses a brain icon, and 我的 uses an outlined person icon

### Requirement: Navigation behavior is preserved
Changing iconography SHALL NOT change labels, selected-state color, route switching, or content behavior.

#### Scenario: Switch destinations
- **WHEN** the user taps any bottom navigation item
- **THEN** the existing destination opens and the selected icon remains highlighted in the existing orange color
