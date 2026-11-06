package com.pandora6ix.app.ui.components

import androidx.compose.runtime.compositionLocalOf

/** 服务端任务状态通知触发当前可见任务页面重新读取权威数据。 */
val LocalTaskRefreshRevision = compositionLocalOf { 0 }
