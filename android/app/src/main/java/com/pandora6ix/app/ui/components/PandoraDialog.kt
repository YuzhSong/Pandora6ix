package com.pandora6ix.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography

/**
 * 统一 AlertDialog（规范第 7 节）：白色 ElevatedSurface、24dp 角、TitleMedium 标题、BodyLarge 正文。
 * 参数与 Material3 AlertDialog 保持一致，既有调用点只需替换函数名；遮罩由主题 backgroundDimAmount 统一为 40%。
 */
@Composable
fun PandoraAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties()
) {
    val c = PandoraTheme.colors
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.widthIn(max = PandoraDimensions.dialogMaxWidth),
        dismissButton = dismissButton,
        icon = icon,
        title = title?.let { t -> { ProvideTextStyle(PandoraTypography.titleMedium) { t() } } },
        text = text?.let { t -> { ProvideTextStyle(PandoraTypography.bodyLarge) { t() } } },
        shape = PandoraShapes.dialog,
        containerColor = c.elevatedSurface,
        iconContentColor = c.textSecondary,
        titleContentColor = c.textPrimary,
        textContentColor = c.textPrimary,
        tonalElevation = 0.dp,
        properties = properties
    )
}

/** 对话框确认动作：Primary 文字；危险操作用 Error（规范：确认 LabelLarge/Primary，危险确认 Error） */
@Composable
fun PandoraDialogConfirm(text: String, onClick: () -> Unit, destructive: Boolean = false, enabled: Boolean = true) {
    val c = PandoraTheme.colors
    TextButton(onClick, enabled = enabled) {
        Text(text, style = PandoraTypography.labelLarge, color = if (!enabled) c.textDisabled else if (destructive) c.error else c.primary)
    }
}

/** 对话框取消动作：TextSecondary，不比确认更强调 */
@Composable
fun PandoraDialogDismiss(text: String = "取消", onClick: () -> Unit, enabled: Boolean = true) {
    val c = PandoraTheme.colors
    TextButton(onClick, enabled = enabled) {
        Text(text, style = PandoraTypography.labelLarge, color = if (enabled) c.textSecondary else c.textDisabled)
    }
}

/**
 * 自定义内容 Dialog 的外壳（日期滚轮、头像菜单、标签编辑等）：
 * 宽度 min(窗口−48dp, 560dp)、白底、24dp 角、24dp 内距。
 */
@Composable
fun PandoraDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    contentPadding: PaddingValues = PaddingValues(PandoraSpacing.dialogPadding),
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(onDismissRequest, properties) {
        PandoraOverlaySurface(
            Modifier.padding(horizontal = PandoraSpacing.space24).widthIn(max = PandoraDimensions.dialogMaxWidth).fillMaxWidth()
        ) {
            Box(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** 统一 BottomSheet：白底、顶部 24dp 角、40% 遮罩、手机全宽/宽屏最大 640dp、默认拖柄 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PandoraBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit
) {
    val c = PandoraTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(max = PandoraDimensions.sheetMaxWidth),
        sheetState = sheetState,
        sheetMaxWidth = PandoraDimensions.sheetMaxWidth,
        shape = PandoraShapes.sheet,
        containerColor = c.elevatedSurface,
        contentColor = c.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = c.scrim,
        content = content
    )
}

/** 统一下拉菜单：白底、12dp 角、3dp 阴影；参数与 Material3 DropdownMenu 兼容 */
@Composable
fun PandoraDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 112.dp, max = 320.dp),
        offset = offset,
        shape = PandoraShapes.medium,
        containerColor = PandoraTheme.colors.elevatedSurface,
        tonalElevation = 0.dp,
        shadowElevation = 3.dp,
        content = content
    )
}
