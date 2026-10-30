package com.pandora6ix.app

import android.content.Context
import android.content.Intent
import cn.jpush.android.api.NotificationMessage
import cn.jpush.android.service.JPushMessageReceiver

/** Explicit SDK callbacks keep cold/warm notification taps and registration rotation connected. */
class PandoraPushReceiver : JPushMessageReceiver() {
    override fun onNotifyMessageArrived(context: Context, message: NotificationMessage) {
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

    override fun onRegister(context: Context, registrationId: String) {
        context.sendBroadcast(Intent(REGISTRATION_CHANGED).setPackage(context.packageName))
    }

    companion object {
        const val REGISTRATION_CHANGED = "com.pandora6ix.app.PUSH_REGISTRATION_CHANGED"
        const val APPROVAL_ARRIVED = "com.pandora6ix.app.APPROVAL_ARRIVED"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
        const val EXTRA_EVENT_TYPE = "eventType"
    }
}
