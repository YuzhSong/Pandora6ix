package com.pandora6ix.app.ui.logs

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.ui.theme.Ink

/** 与表单共用一个滚动视口，按实际光标行定位，避免整个长输入框遮住末尾。 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun JournalEntryField(
    text: String,
    onValueChange: (String) -> Unit,
    colors: TextFieldColors,
    modifier: Modifier = Modifier
) {
    var value by remember { mutableStateOf(TextFieldValue(text)) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var focused by remember { mutableStateOf(false) }
    val requester = remember { BringIntoViewRequester() }
    val interaction = remember { MutableInteractionSource() }
    val density = LocalDensity.current
    val keyboardBottom = WindowInsets.ime.getBottom(density)
    val margin = with(density) { 8.dp.toPx() }

    // 外部切换日期或加载已有日志时同步正文；正常输入保留选区和输入法 composition。
    LaunchedEffect(text) {
        if (value.text != text) value = TextFieldValue(text)
    }
    LaunchedEffect(value, layout, focused, keyboardBottom) {
        if (!focused) return@LaunchedEffect
        val measured = layout ?: return@LaunchedEffect
        if (measured.layoutInput.text.text != value.text) return@LaunchedEffect
        // 等待新行高度应用到外层滚动容器，避免在旧的最大偏移范围内滚动。
        withFrameNanos { }
        val cursor = measured.getCursorRect(value.selection.end.coerceIn(0, value.text.length))
        requester.bringIntoView(Rect(cursor.left, (cursor.top - margin).coerceAtLeast(0f), cursor.right + 1f, cursor.bottom + margin))
    }

    BasicTextField(
        value = value,
        onValueChange = { next ->
            val changed = next.text != value.text
            value = next
            if (changed) onValueChange(next.text)
        },
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
        cursorBrush = SolidColor(Ink),
        minLines = 2,
        interactionSource = interaction,
        onTextLayout = { layout = it },
        decorationBox = { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value.text,
                innerTextField = {
                    // TextLayoutResult 的坐标原点位于文字区域，不包含外框的 padding。
                    Box(Modifier.bringIntoViewRequester(requester)) { innerTextField() }
                },
                enabled = true,
                singleLine = false,
                visualTransformation = VisualTransformation.None,
                interactionSource = interaction,
                placeholder = { Text("记录完成的工作") },
                colors = colors
            )
        }
    )
}
