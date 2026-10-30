package com.pandora6ix.app

import android.content.Context
import android.content.Intent
import cn.jpush.android.api.NotificationMessage
import cn.jpush.android.service.JPushMessageReceiver
import org.json.JSONObject

/** Explicit SDK callbacks keep cold/warm notification taps and registration rotation connected. */
class PandoraPushReceiver : JPushMessageReceiver() {
    override fun onNotifyMessageArrived(context: Context, message: NotificationMessage) {
        onApprovalMessageArrived(context, message)
        // 后台审批使用高优先级渠道；前台消息继续进入统一队列。
        if (MainActivity.isForeground) onForegroundMessageArrived(context, message)
    }

    private fun onApprovalMessageArrived(context: Context, message: NotificationMessage) {
        val extras = runCatching { org.json.JSONObject(message.notificationExtras ?: "{}") }.getOrNull()
        val notificationId = extras?.optString("notificationId").orEmpty().ifBlank { null }
        val eventType = extras?.optString("eventType").orEmpty().ifBlank { null }
        val approval = ApprovalPrompt.from(notificationId, eventType) ?: return
        if (!MainActivity.isForeground) {
            NotificationHelper.notifyApproval(
                context,
                NotificationHelper.idOf(notificationId ?: message.hashCode().toString()),
                "审批通知",
                approval.message,
                approval.notificationId,
                approval.eventType
            )
        }
        context.sendBroadcast(Intent(APPROVAL_ARRIVED).apply {
            setPackage(context.packageName)
            putExtra(EXTRA_NOTIFICATION_ID, approval.notificationId)
            putExtra(EXTRA_EVENT_TYPE, approval.eventType)
        })
    }
    override fun onNotifyMessageOpened(context: Context, message: NotificationMessage) {
        context.startActivity(Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("cn.jpush.android.EXTRA", message.notificationExtras)
        })
    }

    /**
     * 前台收到推送：只把 notificationId 交给页面，由统一的提醒队列决定是否弹窗，
     * 避免这里单独弹一次、轮询又弹一次（标题/正文由页面按 id 从服务端取，extras 里没有）。
     */
    private fun onForegroundMessageArrived(context: Context, message: NotificationMessage) {
        val id = runCatching { JSONObject(message.notificationExtras ?: "{}").optString("notificationId") }.getOrNull()
        if (id.isNullOrBlank()) return
        context.sendBroadcast(
            Intent(MESSAGE_ARRIVED).setPackage(context.packageName)
                .putExtra(EXTRA_NOTIFICATION_ID, id)
                // SDK 的通知栏 id：前台时由页面据此清掉那条系统通知，避免「弹窗 + 通知栏」重复
                .putExtra(EXTRA_NOTIFY_ID, message.notificationId)
        )
    }

    override fun onRegister(context: Context, registrationId: String) {
        context.sendBroadcast(Intent(REGISTRATION_CHANGED).setPackage(context.packageName))
    }

    companion object {
        const val REGISTRATION_CHANGED = "com.pandora6ix.app.PUSH_REGISTRATION_CHANGED"
        const val APPROVAL_ARRIVED = "com.pandora6ix.app.APPROVAL_ARRIVED"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val EXTRA_EVENT_TYPE = "eventType"
        const val MESSAGE_ARRIVED = "com.pandora6ix.app.PUSH_MESSAGE_ARRIVED"
        const val EXTRA_NOTIFY_ID = "notifyId"
    }
}
