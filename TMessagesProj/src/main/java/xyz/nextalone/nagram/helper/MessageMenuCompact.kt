package xyz.nextalone.nagram.helper

import org.telegram.messenger.LocaleController
import org.telegram.ui.ChatActivity
import xyz.nextalone.nagram.NaConfig

/**
 * Manages the per-option "render as icon-only in a compact bar" preference for
 * the chat message context menu. Persisted as a CSV of OPTION_* ints in
 * [NaConfig.compactMessageMenuOptions]. See docs/compact-menu-mock.html for UX.
 */
object MessageMenuCompact {

    /**
     * Options that have special UI / sub-popups / mutable subtext and therefore
     * cannot be rendered as a simple icon button. Keep these always in the full
     * text popup regardless of user preference.
     */
    private val BLACKLIST: Set<Int> = setOf(
        ChatActivity.OPTION_TRANSLATE,                  // swipe-back to translator settings
        ChatActivity.OPTION_REMOVE_ADS,                 // gap separator before it
        ChatActivity.OPTION_ABOUT_REVENUE_SHARING_ADS,
        ChatActivity.OPTION_REPORT_AD,
        ChatActivity.OPTION_HIDE_SPONSORED_MESSAGE,
        ChatActivity.OPTION_TRANSCRIBE,
        ChatActivity.OPTION_CANCEL_SENDING,
        ChatActivity.OPTION_SUGGESTION_EDIT_MESSAGE,
        ChatActivity.OPTION_SUGGESTION_EDIT_PRICE,
        ChatActivity.OPTION_SUGGESTION_EDIT_TIME,
        ChatActivity.OPTION_FACT_CHECK,
        ChatActivity.OPTION_RATE_CALL,
    )

    @JvmStatic
    fun isAllowed(option: Int): Boolean = option !in BLACKLIST

    // ---------- Compact (icon-only) ----------

    @JvmStatic
    fun isCompact(option: Int): Boolean {
        if (!isAllowed(option)) return false
        return getCompactSet().contains(option)
    }

    @JvmStatic
    fun getCompactSet(): Set<Int> {
        return parseCsv(NaConfig.compactMessageMenuOptions.String())
    }

    @JvmStatic
    fun setCompact(option: Int, compact: Boolean) {
        if (!isAllowed(option)) return
        val set = getCompactSet().toMutableSet()
        if (compact) set.add(option) else set.remove(option)
        NaConfig.compactMessageMenuOptions.setConfigString(set.joinToString(","))
    }

    @JvmStatic
    fun setCompact(options: IntArray, compact: Boolean) {
        val set = getCompactSet().toMutableSet()
        for (o in options) {
            if (!isAllowed(o)) continue
            if (compact) set.add(o) else set.remove(o)
        }
        NaConfig.compactMessageMenuOptions.setConfigString(set.joinToString(","))
    }

    // ---------- Hidden ----------

    @JvmStatic
    fun isHidden(option: Int): Boolean = getHiddenSet().contains(option)

    @JvmStatic
    fun getHiddenSet(): Set<Int> {
        return parseCsv(NaConfig.hiddenMessageMenuOptions.String())
    }

    @JvmStatic
    fun setHidden(options: IntArray, hidden: Boolean) {
        val set = getHiddenSet().toMutableSet()
        for (o in options) {
            if (hidden) set.add(o) else set.remove(o)
        }
        NaConfig.hiddenMessageMenuOptions.setConfigString(set.joinToString(","))
    }

    private fun parseCsv(raw: String): Set<Int> {
        if (raw.isEmpty()) return emptySet()
        return raw.split(',').mapNotNullTo(mutableSetOf()) { it.trim().toIntOrNull() }
    }

    @JvmStatic
    fun pickCols(n: Int): Int = if (n <= 4) n.coerceAtLeast(1) else 4

    /**
     * One row in the Message Menu settings dialog. A single label may map to
     * multiple OPTION codes (e.g. SAVE_TO_GALLERY / SAVE_TO_GALLERY2 /
     * SAVE_STICKER_TO_GALLERY are aliases of "Save to gallery"). When the user
     * toggles a row, the change applies to every alias OPTION at once.
     */
    class Candidate(val options: IntArray, private val resId: Int) {
        val primaryOption: Int get() = options[0]
        val label: String get() = LocaleController.getString(resId)
    }
}
