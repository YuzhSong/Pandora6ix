# Spec Delta

## Purpose

为 Web 管理员提供本人资料查看及主题、字号设置，使工作台显示符合个人偏好；资料与组织角色管理相互独立，显示设置按账号保存在本机，退出不删除业务资料或他人的偏好。

## ADDED Requirements

### Requirement: Read own account information
The account page SHALL show the current administrator's name, login name, organization, job title, supervisor and roles as read-only information and SHALL NOT simulate successful password changes without a working authentication service.

#### Scenario: View own account
- **WHEN** the administrator opens account settings
- **THEN** the client displays their directory information and does not offer controls that alter their roles or organization through this page

### Requirement: Account-scoped display preferences
The client SHALL allow light, dark or system theme and small, standard or large font size, persist preferences per account on the current device, and preserve them after logout.

#### Scenario: Reload with dark theme
- **WHEN** the user saves dark theme and large font size then reloads
- **THEN** both preferences remain active for that account

#### Scenario: Switch accounts
- **WHEN** preferences are read for a different account
- **THEN** the other account's saved preferences or defaults are used

### Requirement: Report preference storage failures
The client SHALL keep the previous display preference if persistence fails and SHALL report the failure instead of showing successful saving.

#### Scenario: Storage is full
- **WHEN** the user changes theme but local storage rejects the write
- **THEN** the previous theme remains active and the account page shows an error
