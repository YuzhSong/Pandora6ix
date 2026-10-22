## ADDED Requirements

### Requirement: 头像由服务端存取

系统 SHALL 提供用户头像的上传与下发：`PUT /users/{id}/avatar` 提交压缩后的图片 data URL，`GET /users/{id}/avatar` 返回图片字节。
上传 SHALL 限本人或 ADMIN；读取 SHALL 限同企业登录用户。
服务端 SHALL 校验类型（JPEG/PNG/WEBP）与体积（≤ 768_000 字符），非法内容 SHALL 返回 422；未设置头像 SHALL 返回 **404**。
头像 SHALL 与账号同库存储（不引入外部文件存储），账号数据清除时一并消失。

#### Scenario: 用户更换头像
- **WHEN** 用户在「我的」页从相册或拍照选择照片
- **THEN** 本地立即生效，并压缩为 256px JPEG 上传到 `PUT /users/{id}/avatar`（204）

#### Scenario: 他人查看头像
- **WHEN** 审批人打开一条申请，请求人为其下属
- **THEN** 客户端请求 `GET /users/{下属 id}/avatar` 得到真实头像字节并展示

#### Scenario: 未设置头像
- **WHEN** 请求的用户从未上传头像
- **THEN** `GET /users/{id}/avatar` 返回 404，客户端显示姓名首字占位，SHALL NOT 显示伪造图片

### Requirement: 二进制响应不套统一信封

图片类响应 SHALL 直接输出字节流，SHALL NOT 被包装成 `{data, requestId}`；契约 SHALL 标注为 `image/jpeg|png|webp` 且 `format: binary`。

#### Scenario: 契约与实现一致
- **WHEN** 生成 `openapi.json`
- **THEN** `/users/{id}/avatar` 的 200 响应是二进制 schema，而不是 `{"data": ...}`
