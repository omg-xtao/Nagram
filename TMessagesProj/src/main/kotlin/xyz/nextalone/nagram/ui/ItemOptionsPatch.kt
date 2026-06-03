@file:JvmName("ItemOptionsPatch")

package xyz.nextalone.nagram.ui

import android.view.ViewGroup
import android.widget.LinearLayout
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.RadioButtonCell
import org.telegram.ui.Components.ItemOptions
import java.util.LinkedList
import java.util.WeakHashMap

fun interface RadioItemClickListener {
    fun onClick(cell: RadioButtonCell)
}

fun ItemOptions.addTitle(title: CharSequence, subTitle: CharSequence? = null): HeaderCell {
    val titleText = if (title is String) AndroidUtilities.replaceTags(title) else title
    this.addText(titleText, 15, AndroidUtilities.bold(), -1)
    if (subTitle != null) {
        this.addText(subTitle, 13, null, -1)
    }
    return HeaderCell(this.context)
}

private val radioGroupMap = WeakHashMap<ItemOptions, MutableList<RadioButtonCell>>()

private fun ItemOptions.getRadioGroup(): MutableList<RadioButtonCell> {
    synchronized(radioGroupMap) {
        var group = radioGroupMap[this]
        if (group == null) {
            group = LinkedList()
            radioGroupMap[this] = group
        }
        return group
    }
}

fun ItemOptions.doRadioCheck(cell: RadioButtonCell) {
    if (!cell.isChecked) {
        getRadioGroup().forEach {
            if (it.isChecked) {
                it.setChecked(false, true)
            }
        }
        cell.setChecked(true, true)
    }
}

fun ItemOptions.addRadioItem(
    text: String,
    value: Boolean,
    valueText: String? = null,
    listener: RadioItemClickListener
): RadioButtonCell {
    val ctx = this.context
    val checkBoxCell = RadioButtonCell(ctx, true)
    checkBoxCell.setBackgroundDrawable(Theme.getSelectorDrawable(false))
    checkBoxCell.minimumHeight = AndroidUtilities.dp(50f)
    this.addView(checkBoxCell, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    if (valueText == null) {
        checkBoxCell.setTextAndValue(text, true, value)
    } else {
        checkBoxCell.setTextAndValueAndCheck(text, valueText, true, value)
    }
    getRadioGroup().add(checkBoxCell)
    checkBoxCell.setOnClickListener {
        listener.onClick(checkBoxCell)
    }
    return checkBoxCell
}

fun ItemOptions.addRadioItems(
    text: Array<String>,
    value: (Int, String) -> Boolean,
    valueText: ((Int, String) -> String)? = null,
    listener: (index: Int, text: String, cell: RadioButtonCell) -> Unit
): List<RadioButtonCell> {
    val list = mutableListOf<RadioButtonCell>()
    text.forEachIndexed { index, textI ->
        list.add(this.addRadioItem(textI, value(index, textI), valueText?.invoke(index, textI), RadioItemClickListener { cell ->
            listener(index, textI, cell)
        }))
    }
    return list
}
