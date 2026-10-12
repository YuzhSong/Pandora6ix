# Spec Delta

## Purpose

为本企业管理员提供企业资料、组织结构、人员与固定业务角色的基础维护演示，区分组织隶属关系与人员直接管理关系，使后续正式身份和任务审批接口具备可评审的管理端交互基础。

## ADDED Requirements

### Requirement: Create within the selected organization
Organization details SHALL offer personnel creation with the selected organization prefilled and child-unit creation with the selected company or department prefilled as parent. The editor SHALL retain the ability to change the proposed relationship and SHALL apply existing hierarchy validation.

#### Scenario: Add a team under a department
- **WHEN** an administrator selects a department and opens child-unit creation
- **THEN** the editor defaults to a team belonging to that department and saving follows the existing version and hierarchy rules

### Requirement: Current enterprise and organization maintenance
The administration client SHALL show and edit the current demo enterprise's name and organization units, preserving valid parent relationships and rejecting self-parenting or cycles.

#### Scenario: Change a department parent
- **WHEN** an administrator selects the unit itself or one of its descendants as its parent
- **THEN** saving is rejected and the original organization remains unchanged

### Requirement: Personnel and supervisor maintenance
The client SHALL allow searching, filtering, adding and editing personnel with independent organization, job title, login name and direct supervisor fields, rejecting duplicate login names and supervisory cycles.

#### Scenario: Create personnel
- **WHEN** an administrator saves valid personnel details
- **THEN** the person is visible in the directory and remains after reloading the local demonstration

#### Scenario: Invalid supervisor
- **WHEN** a person is assigned themselves or a management descendant as supervisor
- **THEN** saving is rejected without changing existing personnel

### Requirement: Fixed role assignment
The client SHALL configure personnel roles from ADMIN, FOUNDER, DEPARTMENT_MANAGER, TEAM_LEAD and EMPLOYEE and SHALL NOT infer business read or approval permissions from administrator status.

#### Scenario: Configure a team lead
- **WHEN** an administrator selects TEAM_LEAD for a person and saves
- **THEN** the directory displays that role without granting the administrator daily task or journal operations

### Requirement: Safe local demonstration storage
Demo directory data SHALL be separate from the demonstration session, contain no passwords, and SHALL report storage failures instead of claiming a successful durable save.

#### Scenario: Browser storage unavailable
- **WHEN** a write cannot be persisted in browser storage
- **THEN** the UI reports failure and does not show a successful save

### Requirement: Separate profile and role maintenance
Personnel forms SHALL maintain profile and organizational relationships without changing existing roles. New personnel SHALL start with EMPLOYEE. Dedicated role configuration SHALL update only a selected person’s roles, require at least one valid role and reject stale versions without altering profile fields. Organization creation SHALL preselect an eligible parent; COMPANY root editing SHALL not request a parent.

#### Scenario: Configure roles independently
- **WHEN** an administrator saves a role selection
- **THEN** profile and organizational fields remain unchanged and stale or invalid selections are rejected

### Requirement: Company-scoped local administration
Administrators SHALL operate only their bound local company. The client SHALL NOT expose a company list or switching actions. Existing catalog data SHALL remain preserved, with its initially bound company retained despite later activeId changes. Company names and root names SHALL stay synchronized.

#### Scenario: Read a legacy catalog
- **WHEN** the client first opens an existing multi-company catalog
- **THEN** it retains the current company binding, preserves other directories without exposing them and writes only the bound company's data

### Requirement: First company initialization
A browser without company data SHALL show a company initialization form and establish only the company root and administrator profile. Existing company data SHALL prevent repeated initialization. Storage failures SHALL not report success. The form SHALL NOT collect passwords or claim server authentication.

#### Scenario: Initialize once
- **WHEN** an administrator initializes an empty workspace
- **THEN** the company root and administrator are saved and subsequent visits enter that company without offering another creation

### Requirement: Grouped personnel cards
Personnel SHALL group by organization or highest configured business role, with administrators first and expandable rows of account cards. Job title text SHALL NOT determine role authority.

#### Scenario: Group accounts by role
- **WHEN** personnel are grouped by role
- **THEN** each account belongs to its highest configured role group and the administrator group appears first
