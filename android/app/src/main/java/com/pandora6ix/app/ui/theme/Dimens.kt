package com.pandora6ix.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp 基础单位、8dp 主节奏（规范第 4 节） */
object PandoraSpacing {
    val space2 = 2.dp
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space20 = 20.dp
    val space24 = 24.dp
    val space32 = 32.dp
    val space40 = 40.dp
    val space48 = 48.dp

    /** 页面左右内容边距 */
    val pageHorizontal = space16
    /** 普通面板内距 */
    val panelPadding = space16
    /** 标题到内容 */
    val titleToContent = space12
    /** 普通分区间距 */
    val section = space24
    /** 汇总/成员详情紧凑面板之间 */
    val compactPanelGap = space16
    /** 任务/日志独立条目内距：横 12、纵 8 */
    val itemHorizontal = space12
    val itemVertical = space8
    /** 弹窗内容内距 */
    val dialogPadding = space24
    /** 登录页水平边距 */
    val loginHorizontal = space24
}

object PandoraDimensions {
    val borderWidth = 1.dp
    val focusWidth = 2.dp
    val iconSmall = 16.dp
    val icon = 24.dp
    val touchTarget = 48.dp
    val buttonHeight = 48.dp
    val compactButtonHeight = 40.dp
    val chipButtonHeight = 32.dp
    val textFieldMinHeight = 56.dp
    val navigationBarHeight = 80.dp
    val topBarMinHeight = 64.dp
    val avatarSmall = 24.dp
    val avatarMedium = 32.dp
    val avatarList = 40.dp
    val avatarLarge = 48.dp
    val avatarProfile = 64.dp
    val placeholderIcon = 64.dp
    val dialogMaxWidth = 560.dp
    val sheetMaxWidth = 640.dp
    val loginMaxWidth = 480.dp
    val wheelRowHeight = 48.dp
}

/** 四列栅格：外边距 16、列间 8；嵌入面板时再扣面板左右内距 */
object PandoraGrid {
    val margin = 16.dp
    val gutter = 8.dp
    fun columnWidth(availableWidth: Dp, columns: Int = 4, margin: Dp = PandoraGrid.margin): Dp =
        (availableWidth - margin * 2 - gutter * (columns - 1)) / columns
}

/** 日历是规范允许的几何例外，集中在此而不写在页面里 */
object PandoraCalendarDimens {
    val contentHorizontal = 8.dp
    val timeAxisWidth = 40.dp
    val dayHourHeight = 60.dp
    val weekHourHeight = 56.dp
    val monthBarMinHeight = 18.dp
    val monthBarRadius = 3.dp
    /** 日视图时间块与周视图任务卡：窄列内保留 4dp 小角 */
    val blockRadius = 4.dp
}
