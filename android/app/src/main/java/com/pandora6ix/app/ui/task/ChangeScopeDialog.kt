package com.pandora6ix.app.ui.task

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pandora6ix.app.data.businessTodayKey

@Composable
fun ChangeScopeDialog(date: String, timezone: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val label = if (date == businessTodayKey(timezone)) "今天" else date.replace('-', '/')
    AlertDialog(onDismissRequest = onDismiss, title = { Text("修改范围") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                TextButton({ onSelect("SINGLE") }, Modifier.fillMaxWidth()) { Text("仅修改${label}的") }
                TextButton({ onSelect("THIS_AND_FUTURE") }, Modifier.fillMaxWidth()) { Text("修改${label}及未来的") }
            }
        }, confirmButton = {}, dismissButton = { TextButton(onDismiss) { Text("取消") } })
}
