package com.pandora6ix.app.ui.logs

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography
import io.noties.markwon.Markwon
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import org.commonmark.node.Node

/** Read-only rendering; the editor and API continue to use the original source. */
internal fun journalMarkdownRenderer(context: Context, primary: Int, background: Int): Markwon =
    Markwon.builder(context)
        .usePlugin(SoftBreakAddsNewLinePlugin.create())
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(TaskListPlugin.create(primary, primary, background))
        .usePlugin(object : AbstractMarkwonPlugin() {
            override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                builder.blockHandler(object : MarkwonVisitor.BlockHandler {
                    override fun blockStart(visitor: MarkwonVisitor, node: Node) {
                        visitor.ensureNewLine()
                    }

                    override fun blockEnd(visitor: MarkwonVisitor, node: Node) {
                        if (visitor.hasNext(node)) visitor.ensureNewLine()
                    }
                })
            }
        })
        .build()

@Composable
internal fun JournalMarkdown(
    source: String,
    modifier: Modifier = Modifier,
    style: TextStyle = PandoraTypography.bodyLarge,
    color: Color = PandoraTheme.colors.textPrimary,
    maxLines: Int = Int.MAX_VALUE,
    linksEnabled: Boolean = true
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val primary = PandoraTheme.colors.primary.toArgb()
    val background = PandoraTheme.colors.appBackground.toArgb()
    val renderer = remember(context, primary, background) { journalMarkdownRenderer(context, primary, background) }
    val rendered = remember(renderer, source) { renderer.toMarkdown(journalMarkdownSource(source)) }
    val textSizePx = with(density) { style.fontSize.toPx() }
    val lineHeightPx = with(density) { style.lineHeight.toPx() }
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { TextView(it).apply { includeFontPadding = false } },
        update = { view ->
            view.setTextColor(color.toArgb())
            view.setLinkTextColor(primary)
            view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
            val metrics = view.paint.fontMetrics
            view.setLineSpacing((lineHeightPx - (metrics.descent - metrics.ascent)).coerceAtLeast(0f), 1f)
            view.maxLines = maxLines
            view.ellipsize = if (maxLines == Int.MAX_VALUE) null else TextUtils.TruncateAt.END
            renderer.setParsedMarkdown(view, rendered)
            // Core adds link movement automatically; previews must defer taps to their card.
            if (!linksEnabled) {
                view.movementMethod = null
                view.isClickable = false
                view.isLongClickable = false
                view.isFocusable = false
            }
        }
    )
}
