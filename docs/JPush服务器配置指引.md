# JPush 服务器配置指引

本指南说明 Pandora 的推送配置与验收。AppKey 从极光控制台读取，Master Secret 仅由服务器管理员在受控终端配置。原 Word 文件含真实凭据及控制台截图，已从本次待合并提交移除；其中的旧 Master Secret 必须在极光控制台轮换。

## 服务端配置

由有 sudo 权限的运维人员编辑 `/opt/Pandora/config/backend.env`，保留已有数据库及其他变量，添加以下变量并填写轮换后的值：

```text
PANDORA_JPUSH_APP_KEY=<AppKey>
PANDORA_JPUSH_MASTER_SECRET=<新 Master Secret>
```

环境文件应归 root 所有，权限为 600。配置后重启 `pandora-backend.service` 并检查服务状态、健康接口与必要错误日志。不要输出环境变量、Master Secret 或含凭据的截图。受限账号 pandora-deploy 和运行账号 pandora-app 不需要增加运维权限。

未配置推送凭据时，后端不调用极光，站内通知和其他业务继续工作。合并及部署代码不代表已完成服务商配置或真机推送验收。

## Android 构建与验收

通过本地 Gradle 属性或环境变量 `JPUSH_APP_KEY` 提供同一个 AppKey，重新构建并安装 APK。Master Secret 不得进入 Android、Git、发布包或聊天。

工程还必须保留 `PandoraPushService : JCommonService` 和 Manifest 中的服务声明（启用、非导出、`:pushcore` 进程及 `cn.jiguang.user.service.action`）。若日志出现 `AndroidManifest.xml missing required service:cn.jpush.android.service.JCommonService`，需要修复组件声明并重新构建安装；仅重新填写 AppKey 不会解决。配置要求参见[极光 Android 快速接入](https://docs.jiguang.cn/jpush/quickstart/Android_quick)。

用专门测试账号和真机验收：允许系统通知，确认设备注册，验证审批申请及通过/驳回的通知，再验证前台、后台、冷启动和通知点击。关闭通知、拒绝权限、切换账号及退出后，应停止向不再绑定的安装实例投递。测试仅使用测试人员，避免向真实员工发送验收消息。

送达依赖网络、系统通知许可及厂商后台策略；强制停止和省电策略可能拦截推送。站内通知记录是通知内容的可靠来源。
