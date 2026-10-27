package com.pandora6ix.app

import android.content.Context
import android.content.Intent
import cn.jpush.android.api.NotificationMessage
import cn.jpush.android.service.JPushMessageReceiver

/** Explicit SDK callbacks keep cold/warm notification taps and registration rotation connected. */
class PandoraPushReceiver : JPushMessageReceiver() {
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
    }
}
