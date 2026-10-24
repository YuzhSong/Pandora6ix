# Tasks: add-user-avatar

## 1. 服务端存取

- [x] 1.1 迁移 V7：`app_user.avatar_image CLOB`
- [x] 1.2 `Contracts.UserAvatar`（dataUrl，上限 768_000）
- [x] 1.3 `DirectoryService.saveAvatar`：本人或管理员、MIME/base64 校验
- [x] 1.4 `DirectoryService.avatar`：未设置返回 null → 控制器 404
- [x] 1.5 `AvatarController`：GET 二进制、PUT 204

## 2. 契约与信封

- [x] 2.1 `BusinessResponses` 对 `byte[]` 不套信封
- [x] 2.2 `generate-openapi.py`：`BINARY_RESPONSES` + 计数校验
- [x] 2.3 重生成 `openapi.json`（58 operations）

## 3. 客户端

- [x] 3.1 `ApiClient.getBytes`（404→null、401 会话失效）
- [x] 3.2 `UserRepository`：按人缓存（含 null）、压缩上传
- [x] 3.3 「我的」页选图/拍照后同步上传
- [x] 3.4 审批详情 `RemoteAvatar` 显示真实头像，失败回退首字

## 4. 验证

- [x] 4.1 后端全量 `mvn test` 93 项通过（含新增头像用例）
- [x] 4.2 `:app:compileDebugKotlin` 通过
- [ ] 4.3 真机验收：换头像后审批详情能看到本人与他人真实头像
