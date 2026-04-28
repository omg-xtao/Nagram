package xyz.nextalone.nagram.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import java.util.function.IntConsumer
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme

/**
 * 3-segment slider used by the Message Menu dialog: HIDE / TEXT / ICON.
 *
 * Pass per-segment enabled flags so callers can grey out unsupported states
 * (e.g. items without a hide toggle, or items in the compact blacklist).
 */
@SuppressLint("ViewConstructor")
class SegmentedThreeStateView(
    context: Context,
    private val labels: Array<String>,
    private val enabled: BooleanArray,
) : LinearLayout(context) {

    var current: Int = 0
        private set

    var onChange: IntConsumer? = null

    private val cells: Array<TextView>

    init {
        orientation = HORIZONTAL
        val pad = AndroidUtilities.dp(2f)
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply {
            cornerRadius = AndroidUtilities.dp(8f).toFloat()
            setColor(Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), 0.08f))
        }
        cells = Array(labels.size) { i ->
            TextView(context).apply {
                text = labels[i]
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(AndroidUtilities.dp(8f), AndroidUtilities.dp(4f), AndroidUtilities.dp(8f), AndroidUtilities.dp(4f))
                isAllCaps = false
                isClickable = enabled[i]
                isFocusable = enabled[i]
                if (!enabled[i]) {
                    alpha = 0.3f
                } else {
                    setOnClickListener {
                        if (current == i) return@setOnClickListener
                        setSelection(i, true)
                    }
                }
                this@SegmentedThreeStateView.addView(this, LayoutParams(0, AndroidUtilities.dp(28f), 1f).apply {
                    if (i > 0) leftMargin = AndroidUtilities.dp(2f)
                })
            }
        }
        applyStyles()
    }

    fun setSelection(index: Int, fire: Boolean) {
        if (index !in labels.indices) return
        if (!enabled[index]) return
        current = index
        applyStyles()
        if (fire) onChange?.accept(index)
    }

    private fun applyStyles() {
        val activeBg = GradientDrawable().apply {
            cornerRadius = AndroidUtilities.dp(6f).toFloat()
            setColor(Theme.getColor(Theme.key_featuredStickers_addButton))
        }
        cells.forEachIndexed { i, tv ->
            if (i == current) {
                tv.background = activeBg.constantState?.newDrawable()?.mutate()
                tv.setTextColor(Color.WHITE)
            } else {
                tv.background = null
                tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText))
            }
        }
    }
}
