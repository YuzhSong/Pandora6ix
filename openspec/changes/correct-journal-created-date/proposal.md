## Why

日志详情的创建时间将 workDate 与实际创建时刻拼接，补交日志时日期错误。

## What Changes

- 创建时间的日期与时分统一取 publishedAt，按公司业务时区展示。
- 无创建时间时显示未知，不使用日志所属日期替代。

## Capabilities

### New Capabilities

- `journal-creation-time-display`: 日志详情实际创建时刻展示。

### Modified Capabilities

无。

## Impact

Android 日志详情展示；无需服务端或 API 变更。
