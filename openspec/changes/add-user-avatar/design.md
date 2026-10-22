## 为什么是 data URL 存 CLOB，而不是文件/OSS

- 头像随账号生命周期走：账号删除、离职停用、企业数据导出都不该留下"孤儿文件"。
- 项目没有对象存储依赖，也不该为一个头像引入 multipart 配置与文件清理任务。
- 客户端已把图片压到 256px / JPEG 82，单张约 10–30KB，CLOB 完全够用；服务端只做类型与体积校验，不做图像处理。

代价是响应体略大（base64 约 +33%），但请求频率低（按 userId 缓存后一人一次），可接受。

## 信封问题（踩到的坑）

`BusinessResponses` 是 `@RestControllerAdvice` 的 `ResponseBodyAdvice`，会把**任何** body 包成 `{data, requestId}`。直接返回 `byte[]` 会被 Jackson 当成可序列化对象包一层，图片就废了。

两处同步修改：

```java
// BusinessResponses.beforeBodyWrite
if (body instanceof byte[]) return body;   // 二进制不套信封
```

```python
# generate-openapi.py
BINARY_RESPONSES = {("GET", "/users/{id}/avatar")}
```
生成器原本要求每条路由都在 `RESPONSES` 表里有 JSON schema，二进制路由没有 schema 概念，因此单独成集合；计数校验相应改为 `len(RESPONSES) + len(BINARY_RESPONSES) + 4`。

## 客户端加载与回退

`UserRepository.avatar(userId)`：

- `HashMap<String, Bitmap?>` 缓存，**含"未设置"的 null 结果**——否则每进入一次详情都会重打一次 404 请求
- 用 `Mutex` 保护（多人并发进入详情时避免重复下载）
- 失败一律回退，不影响审批主流程

`RemoteAvatar` 组件：加载中先显示姓名首字，加载成功换成 `Image(bitmap.asImageBitmap(), contentScale = Crop)`；没有 `userRepo`（如预览/未登录场景）时保持首字占位。

没有引入 Coil：项目依赖只有 okhttp + compose，`getBytes` + `BitmapFactory` 就够了，避免为一张头像加图片库。

## 上传链路

「我的」页 `importImage(uri)`：

1. 原图拷到 `filesDir/avatars`（本地立即生效，弱网也能看）
2. `uploadAvatarFile(uri)`：IO 线程 `decodeUriThumbnail(context, uri, 256)` 降采样 → `UserRepository.uploadAvatar(userId, bitmap)` 压缩 JPEG 82 → base64 data URL → `PUT`
3. 上传失败只提示"头像上传失败，本地已生效"，不回滚本地选择

## 验证

- 后端：`avatarUploadsAndServesRealImageBytes` 覆盖上传 204、读取字节一致、未设置 404、他人不可代改 403、非图片 data URL 422；全量 93 项通过（新增 V7 迁移在所有测试上下文中正常执行）。
- Android：`:app:compileDebugKotlin` 通过。
