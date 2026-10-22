## Why

「我的」页能换头像，但头像只存在**本机**（`filesDir/avatars` + SharedPreferences），服务端完全没有头像数据。于是审批详情里的请求人、审批人、执行人只能画一个「姓名首字圆形占位」——用户看到的是假头像，而且看不到别人的真实头像。

需求是审批详情要显示真实的姓名、职位与头像。职位可以直接读 `app_user.job_title`；头像必须先让服务端有这份数据。

## What Changes

- **存储**：迁移 `V7__user_avatar.sql` 给 `app_user` 加 `avatar_image CLOB`，存客户端压缩后的图片 data URL（`data:image/jpeg|png|webp;base64,...`）。不引入文件系统/OSS：头像与账号同库、随账号删除/离职一起走，避免头像与其他业务数据分处两地。
- **接口**（`AvatarController`）：
  - `PUT /users/{id}/avatar`：`{dataUrl}`，本人或管理员；校验前缀、MIME ∈ {jpeg,png,webp}、base64 可解码；上限 768_000 字符（约 560KB）
  - `GET /users/{id}/avatar`：同企业登录用户可读（审批详情要显示他人头像），未设置返回 **404**，客户端回退姓名首字
- **响应包装**：`BusinessResponses` 对 `byte[]` 不再套 `{data, requestId}` 信封，直接输出字节流；`generate-openapi.py` 把该路由标为二进制响应（`BINARY_RESPONSES`），契约里是 `image/*` + `format: binary`。
- **Android**：新增 `data/UserRepository`（按人缓存头像 Bitmap，含"未设置"的 null 结果；上传时压缩到 256px JPEG 质量 82）；`ApiClient.getBytes()` 二进制读取（404 → null、401 走会话失效）；「我的」页选图/拍照后本地生效并同步上传；审批详情的 `PersonRow`/`PersonChip` 用 `RemoteAvatar` 显示真实头像 + 姓名 + 职位。

## Capabilities

### New Capabilities
- `user-avatar`: 用户头像的服务端存取（上传、下发、未设置 404）与客户端展示/回退。

### Modified Capabilities
（无）

## Impact

- 后端：新增 `V7` 迁移、`AvatarController`、`DirectoryService.saveAvatar/avatar`、`Contracts.UserAvatar`、`BusinessResponses` 二进制放行、`openapi.json`（+305 行）、测试 `avatarUploadsAndServesRealImageBytes`。
- Android：新增 `data/UserRepository.kt`、`ApiClient.getBytes`、`MainActivity`（注入 UserRepository、我的页上传）、`ui/review/ReviewScreen.kt`（RemoteAvatar、职位）。
- 兼容：头像字段可空，未设置时旧客户端不受影响；审批响应不内嵌头像，列表请求体积不变。
- 隐私：头像仅同企业可见，随 token 鉴权；不生成外链，换设备重新拉取。
